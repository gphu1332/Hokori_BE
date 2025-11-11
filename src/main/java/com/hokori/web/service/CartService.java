package com.hokori.web.service;

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
    private final UserRepository userRepo;
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
        List<CartItem> items = itemRepo.findByCart_Id(cart.getId());

        List<CartItemResponse> itemDtos = items.stream()
                .map(ci -> new CartItemResponse(
                        ci.getId(),
                        ci.getCourse().getId(),
                        ci.getQuantity(),
                        ci.getTotalPrice(),
                        ci.getSelected()
                ))
                .toList();

        long subtotal = itemRepo.sumSelectedTotal(cart.getId());
        return new CartResponse(cart.getId(), itemDtos, subtotal);
    }

    public CartResponse add(AddItemRequest req) {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        int qty = (req.quantity() == null || req.quantity() < 1) ? 1 : req.quantity();

        if (enrollmentRepo.existsByUserIdAndCourseId(userId, req.courseId())) {
            throw new IllegalStateException("COURSE_OWNED");
        }

        Course course = courseRepo.findById(req.courseId())
                .orElseThrow(() -> new IllegalArgumentException("COURSE_NOT_FOUND"));
        long price = course.getPriceCents(); // đổi getter nếu tên khác

        CartItem item = itemRepo.findByCart_IdAndCourse_Id(cart.getId(), course.getId())
                .map(ci -> {
                    ci.setQuantity(ci.getQuantity() + qty);
                    ci.setTotalPrice(price * ci.getQuantity());
                    return ci;
                })
                .orElseGet(() -> {
                    CartItem ci = new CartItem();
                    ci.setCart(cart);
                    ci.setCourse(course);
                    ci.setQuantity(qty);
                    ci.setTotalPrice(price * qty);
                    return ci;
                });

        itemRepo.save(item);
        return view();
    }

    public CartResponse update(Long itemId, UpdateItemRequest req) {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        CartItem item = itemRepo.findById(itemId)
                .filter(ci -> ci.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new IllegalArgumentException("ITEM_NOT_FOUND"));

        if (req.quantity() != null) {
            if (req.quantity() < 1) throw new IllegalArgumentException("BAD_QUANTITY");
            long price = item.getCourse().getPriceCents(); // đổi getter nếu khác
            item.setQuantity(req.quantity());
            item.setTotalPrice(price * req.quantity());
        }
        if (req.selected() != null) item.setSelected(req.selected());

        itemRepo.save(item);
        return view();
    }

    public CartResponse remove(Long itemId) {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        CartItem item = itemRepo.findById(itemId)
                .filter(ci -> ci.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new IllegalArgumentException("ITEM_NOT_FOUND"));

        itemRepo.delete(item);
        return view();
    }

    public CartResponse selectAll(boolean selected) {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        cart.getItems().forEach(i -> i.setSelected(selected));
        return view();
    }

    public CartResponse clear() {
        Long userId = current.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear(); // orphanRemoval=true sẽ xóa con
        return view();
    }
}
