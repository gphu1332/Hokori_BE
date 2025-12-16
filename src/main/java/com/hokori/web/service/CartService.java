package com.hokori.web.service;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.dto.cart.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository itemRepo;
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final CurrentUserService current;  // lấy user hiện tại
    private final EntityManager em;

    private Cart getOrCreateCart(Long userId) {
        return cartRepo.findByUser_Id(userId).orElseGet(() -> {
            Cart c = new Cart();
            // không hit DB: getReference (nếu thích có thể dùng userRepo.getReferenceById)
            c.setUser(em.getReference(User.class, userId));
            return cartRepo.save(c);
        });
    }

    public CartResponse view() {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        
        // Use native query to avoid LOB stream error when loading Course entity
        List<Object[]> itemsMeta = itemRepo.findCartItemMetadataByCartId(cart.getId());

        // Filter out invalid items (deleted, unpublished, or already enrolled courses)
        List<CartItemResponse> itemDtos = itemsMeta.stream()
                .map(meta -> {
                    // Handle nested array case (PostgreSQL)
                    Object[] actualMeta = meta;
                    if (meta.length == 1 && meta[0] instanceof Object[]) {
                        actualMeta = (Object[]) meta[0];
                    }
                    // Metadata: [id, courseId, quantity, totalPrice, selected, courseStatus, courseDeletedFlag,
                    //           courseTitle, courseSlug, coverImagePath, teacherName]
                    Long itemId = ((Number) actualMeta[0]).longValue();
                    Long courseId = ((Number) actualMeta[1]).longValue();
                    Integer quantity = ((Number) actualMeta[2]).intValue();
                    Long totalPrice = ((Number) actualMeta[3]).longValue();
                    Boolean selected = actualMeta[4] instanceof Boolean ? 
                        (Boolean) actualMeta[4] : 
                        ((Number) actualMeta[4]).intValue() != 0;
                    String courseStatus = actualMeta[5] != null ? actualMeta[5].toString() : null;
                    Boolean courseDeletedFlag = actualMeta[6] instanceof Boolean ? 
                        (Boolean) actualMeta[6] : 
                        ((Number) actualMeta[6]).intValue() != 0;
                    String courseTitle = actualMeta[7] != null ? actualMeta[7].toString() : null;
                    String courseSlug = actualMeta[8] != null ? actualMeta[8].toString() : null;
                    String coverImagePath = actualMeta[9] != null ? actualMeta[9].toString() : null;
                    String teacherName = actualMeta[10] != null ? actualMeta[10].toString() : null;
                    
                    return new Object[]{itemId, courseId, quantity, totalPrice, selected, courseStatus, courseDeletedFlag,
                                       courseTitle, courseSlug, coverImagePath, teacherName};
                })
                .filter(meta -> {
                    // Filter: remove deleted courses, unpublished courses, and courses user already enrolled
                    Boolean deletedFlag = (Boolean) meta[6];
                    String statusStr = (String) meta[5];
                    Long courseId = (Long) meta[1];
                    
                    if (deletedFlag) return false;
                    
                    CourseStatus status;
                    try {
                        status = statusStr != null ? CourseStatus.valueOf(statusStr.toUpperCase()) : null;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                    // Allow PUBLISHED and PENDING_UPDATE courses in cart
                    if (status != CourseStatus.PUBLISHED && status != CourseStatus.PENDING_UPDATE) return false;
                    
                    // Check if user already enrolled (remove from cart if enrolled)
                    if (enrollmentRepo.existsByUser_IdAndCourse_Id(userId, courseId)) {
                        return false;
                    }
                    
                    return true;
                })
                .map(meta -> {
                    Long itemId = (Long) meta[0];
                    Long courseId = (Long) meta[1];
                    Integer quantity = (Integer) meta[2];
                    Long totalPrice = (Long) meta[3];
                    Boolean selected = (Boolean) meta[4];
                    String courseTitle = (String) meta[7];
                    String courseSlug = (String) meta[8];
                    String coverImagePath = (String) meta[9];
                    String teacherName = (String) meta[10];
                    return new CartItemResponse(itemId, courseId, quantity, totalPrice, selected,
                                                courseTitle, courseSlug, coverImagePath, teacherName);
                })
                .toList();
        
        // Clean up invalid items from database (async cleanup)
        cleanupInvalidCartItems(cart.getId(), userId);

        long subtotal = itemDtos.stream()
                .filter(CartItemResponse::selected)
                .mapToLong(CartItemResponse::totalPrice)
                .sum();
        
        return new CartResponse(cart.getId(), itemDtos, subtotal);
    }
    
    /**
     * Lấy thông tin checkout đơn giản - chỉ cartId và courseIds đã được chọn
     * Dùng để đơn giản hóa request khi thanh toán
     */
    public CartCheckoutInfo getCheckoutInfo() {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        
        // Get selected course IDs
        List<Long> selectedCourseIds = view().items().stream()
                .filter(CartItemResponse::selected)
                .map(CartItemResponse::courseId)
                .toList();
        
        return new CartCheckoutInfo(cart.getId(), selectedCourseIds);
    }
    
    /**
     * Remove invalid cart items (deleted/unpublished courses or courses user already enrolled).
     * This is called after view() to clean up the cart.
     */
    private void cleanupInvalidCartItems(Long cartId, Long userId) {
        List<Object[]> itemsMeta = itemRepo.findCartItemMetadataByCartId(cartId);
        List<Long> itemsToDelete = itemsMeta.stream()
                .map(meta -> {
                    Object[] actualMeta = meta;
                    if (meta.length == 1 && meta[0] instanceof Object[]) {
                        actualMeta = (Object[]) meta[0];
                    }
                    Long itemId = ((Number) actualMeta[0]).longValue();
                    Long courseId = ((Number) actualMeta[1]).longValue();
                    String courseStatus = actualMeta[5] != null ? actualMeta[5].toString() : null;
                    Boolean courseDeletedFlag = actualMeta[6] instanceof Boolean ? 
                        (Boolean) actualMeta[6] : 
                        ((Number) actualMeta[6]).intValue() != 0;
                    
                    // Check if should be deleted
                    if (courseDeletedFlag) return itemId;
                    
                    CourseStatus status;
                    try {
                        status = courseStatus != null ? CourseStatus.valueOf(courseStatus.toUpperCase()) : null;
                    } catch (IllegalArgumentException e) {
                        return itemId;
                    }
                    // Allow PUBLISHED and PENDING_UPDATE courses in cart
                    if (status != CourseStatus.PUBLISHED && status != CourseStatus.PENDING_UPDATE) return itemId;
                    
                    if (enrollmentRepo.existsByUser_IdAndCourse_Id(userId, courseId)) {
                        return itemId;
                    }
                    
                    return null;
                })
                .filter(itemId -> itemId != null)
                .toList();
        
        // Delete invalid items
        itemsToDelete.forEach(itemId -> {
            try {
                itemRepo.deleteById(itemId);
            } catch (Exception e) {
                // Ignore if already deleted
            }
        });
    }

    public CartResponse add(AddItemRequest req) {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        
        // For courses, quantity must always be 1
        if (req.quantity() != null && req.quantity() != 1) {
            throw new IllegalArgumentException("BAD_QUANTITY: Course quantity must be 1");
        }

        // Check if already enrolled
        if (enrollmentRepo.existsByUser_IdAndCourse_Id(userId, req.courseId())) {
            throw new IllegalStateException("COURSE_OWNED");
        }

        // Use native query to avoid LOB stream error (don't load description field)
        Object[] courseData = courseRepo.findCoursePriceById(req.courseId())
                .orElseThrow(() -> new IllegalArgumentException("COURSE_NOT_FOUND"));
        
        // Handle nested array case (PostgreSQL)
        Object[] actualData = courseData;
        if (courseData.length == 1 && courseData[0] instanceof Object[]) {
            actualData = (Object[]) courseData[0];
        }
        
        // Metadata: [id, priceCents, deletedFlag, status]
        Long courseId = ((Number) actualData[0]).longValue();
        Long priceCents = actualData[1] != null ? ((Number) actualData[1]).longValue() : 0L;
        Boolean deletedFlag = actualData[2] instanceof Boolean ? 
            (Boolean) actualData[2] : 
            ((Number) actualData[2]).intValue() != 0;
        String statusStr = actualData[3] != null ? actualData[3].toString() : null;
        
        // Check deletedFlag (should be false from query, but double-check)
        if (deletedFlag) {
            throw new IllegalArgumentException("COURSE_NOT_FOUND");
        }
        
        // Check course status - only PUBLISHED courses can be added to cart
        CourseStatus status;
        try {
            status = statusStr != null ? CourseStatus.valueOf(statusStr.toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid course status");
        }
        // Allow PUBLISHED and PENDING_UPDATE courses to be added to cart
        if (status != CourseStatus.PUBLISHED && status != CourseStatus.PENDING_UPDATE) {
            throw new IllegalStateException("COURSE_NOT_PUBLISHED");
        }
        
        // Check if course already exists in cart
        if (itemRepo.findByCart_IdAndCourse_Id(cart.getId(), courseId).isPresent()) {
            throw new IllegalStateException("COURSE_ALREADY_IN_CART");
        }

        // Use getReference to avoid loading full entity
        Course courseRef = courseRepo.getReferenceById(courseId);

        // Create new cart item (quantity is always 1 for courses)
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setCourse(courseRef);
        item.setQuantity(1); // Courses can only have quantity = 1
        item.setTotalPrice(priceCents); // Price for 1 course

        itemRepo.save(item);
        return view();
    }

    public CartResponse update(Long itemId, UpdateItemRequest req) {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        // Use native query to get item metadata and verify ownership without loading Course entity
        Object[] itemMeta = itemRepo.findCartItemMetadataById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("ITEM_NOT_FOUND"));
        
        // Handle nested array case (PostgreSQL)
        Object[] actualMeta = itemMeta;
        if (itemMeta.length == 1 && itemMeta[0] instanceof Object[]) {
            actualMeta = (Object[]) itemMeta[0];
        }
        
        // Metadata: [id, courseId, cartId, quantity, totalPrice, selected]
        Long cartIdFromItem = ((Number) actualMeta[2]).longValue();
        if (!cartIdFromItem.equals(cart.getId())) {
            throw new IllegalArgumentException("ITEM_NOT_FOUND");
        }
        
        Long courseId = ((Number) actualMeta[1]).longValue();
        
        // Check if user has enrolled in this course (business rule: can't update cart item for owned course)
        if (enrollmentRepo.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalStateException("COURSE_OWNED");
        }
        
        CartItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("ITEM_NOT_FOUND"));

        if (req.quantity() != null) {
            // For courses, quantity must always be 1
            if (req.quantity() != 1) {
                throw new IllegalArgumentException("BAD_QUANTITY: Course quantity must be 1");
            }
            // Use native query to avoid LOB stream error when accessing course price
            Object[] courseData = courseRepo.findCoursePriceById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("COURSE_NOT_FOUND"));
            
            // Handle nested array case (PostgreSQL)
            Object[] actualData = courseData;
            if (courseData.length == 1 && courseData[0] instanceof Object[]) {
                actualData = (Object[]) courseData[0];
            }
            
            // Metadata: [id, priceCents, deletedFlag, status]
            Long priceCents = actualData[1] != null ? ((Number) actualData[1]).longValue() : 0L;
            Boolean deletedFlag = actualData[2] instanceof Boolean ? 
                (Boolean) actualData[2] : 
                ((Number) actualData[2]).intValue() != 0;
            String statusStr = actualData[3] != null ? actualData[3].toString() : null;
            
            // Check if course still exists and is published
            if (deletedFlag) {
                throw new IllegalArgumentException("COURSE_NOT_FOUND");
            }
            
            CourseStatus status;
            try {
                status = statusStr != null ? CourseStatus.valueOf(statusStr.toUpperCase()) : null;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid course status");
            }
            // Allow PUBLISHED and PENDING_UPDATE courses
            if (status != CourseStatus.PUBLISHED && status != CourseStatus.PENDING_UPDATE) {
                throw new IllegalStateException("COURSE_NOT_PUBLISHED");
            }
            
            // Set quantity to 1 (courses can only have quantity = 1)
            item.setQuantity(1);
            item.setTotalPrice(priceCents); // Price for 1 course
        }
        if (req.selected() != null) item.setSelected(req.selected());

        itemRepo.save(item);
        return view();
    }

    public CartResponse remove(Long itemId) {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        // Use native query to verify ownership without loading Course entity
        Object[] itemMeta = itemRepo.findCartItemMetadataById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("ITEM_NOT_FOUND"));
        
        // Handle nested array case (PostgreSQL)
        Object[] actualMeta = itemMeta;
        if (itemMeta.length == 1 && itemMeta[0] instanceof Object[]) {
            actualMeta = (Object[]) itemMeta[0];
        }
        
        // Metadata: [id, courseId, cartId, quantity, totalPrice, selected]
        Long cartIdFromItem = ((Number) actualMeta[2]).longValue();
        if (!cartIdFromItem.equals(cart.getId())) {
            throw new IllegalArgumentException("ITEM_NOT_FOUND");
        }

        CartItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("ITEM_NOT_FOUND"));
        itemRepo.delete(item);
        return view();
    }

    public CartResponse selectAll(boolean selected) {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        // Use native query to avoid loading items and Course entities
        itemRepo.updateSelectedStatusByCartId(cart.getId(), selected);
        return view();
    }

    public CartResponse clear() {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        // Use native query to avoid loading items and Course entities
        itemRepo.deleteAllByCartId(cart.getId());
        return view();
    }
    
    /**
     * Remove cart items by course IDs (used after enrolling in free courses)
     * Uses current authenticated user from SecurityContext
     */
    public void clearItems(List<Long> courseIds) {
        Long userId = current.getCurrentUserId();
        clearItemsForUser(userId, courseIds);
    }
    
    /**
     * Remove cart items by course IDs for a specific user (used in webhook context)
     * @param userId User ID whose cart items should be cleared
     * @param courseIds Course IDs to remove from cart
     */
    public void clearItemsForUser(Long userId, List<Long> courseIds) {
        Cart cart = getOrCreateCart(userId);
        // Delete items for these course IDs
        itemRepo.deleteByCart_IdAndCourse_IdIn(cart.getId(), courseIds);
    }
}
