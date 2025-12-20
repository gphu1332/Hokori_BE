package com.hokori.web.service;

import com.hokori.web.dto.cart.CartCheckoutInfo;
import com.hokori.web.dto.cart.CartResponse;
import com.hokori.web.entity.Cart;
import com.hokori.web.entity.User;
import com.hokori.web.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepo;
    @Mock
    private CartItemRepository itemRepo;
    @Mock
    private CourseRepository courseRepo;
    @Mock
    private EnrollmentRepository enrollmentRepo;
    @Mock
    private CurrentUserService current;
    @Mock
    private EntityManager em;

    @InjectMocks
    private CartService cartService;

    /**
     * TC-CART-01
     * User mới view cart → cart rỗng
     */
    @Test
    void view_newUser_emptyCart_success() {
        // given
        Long userId = 1L;

        when(current.getCurrentUserId()).thenReturn(userId);
        when(cartRepo.findByUser_Id(userId)).thenReturn(Optional.empty());

        Cart cart = new Cart();
        cart.setId(100L);

        when(em.getReference(User.class, userId))
                .thenReturn(new User());
        when(cartRepo.save(any(Cart.class)))
                .thenReturn(cart);

        when(itemRepo.findCartItemMetadataByCartId(cart.getId()))
                .thenReturn(Collections.emptyList());

        // when
        CartResponse response = cartService.view();

        // then
        assertNotNull(response);
        assertEquals(cart.getId(), response.cartId());
        assertTrue(response.items().isEmpty());
        assertEquals(0L, response.selectedSubtotal());
    }

    /**
     * TC-CART-02
     * Lấy checkout info khi cart rỗng
     */
    @Test
    void getCheckoutInfo_emptyCart_success() {
        // given
        Long userId = 2L;

        when(current.getCurrentUserId()).thenReturn(userId);
        when(cartRepo.findByUser_Id(userId)).thenReturn(Optional.empty());

        Cart cart = new Cart();
        cart.setId(200L);

        when(em.getReference(User.class, userId))
                .thenReturn(new User());
        when(cartRepo.save(any(Cart.class)))
                .thenReturn(cart);

        when(itemRepo.findCartItemMetadataByCartId(cart.getId()))
                .thenReturn(Collections.emptyList());

        // when
        CartCheckoutInfo info = cartService.getCheckoutInfo();

        // then
        assertNotNull(info);
        assertEquals(cart.getId(), info.cartId());
        assertTrue(info.courseIds().isEmpty());
    }
}
