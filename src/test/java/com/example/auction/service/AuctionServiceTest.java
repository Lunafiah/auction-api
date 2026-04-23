package com.example.auction.service;

import com.example.auction.dto.BidRequest;
import com.example.auction.dto.BidResponse;
import com.example.auction.model.AuctionItem;
import com.example.auction.model.User;
import com.example.auction.repository.AuctionItemRepository;
import com.example.auction.repository.BidHistoryRepository;
import com.example.auction.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionServiceTest {

    @Mock
    private AuctionItemRepository auctionItemRepository;

    @Mock
    private BidHistoryRepository bidHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AuctionService auctionService;

    private AuctionItem item;
    private User user;

    @BeforeEach
    void setUp() {
        item = new AuctionItem();
        item.setId(1L);
        item.setStartingPrice(new BigDecimal("100"));
        item.setCurrentHighestBid(new BigDecimal("150"));
        item.setEndTime(LocalDateTime.now().plusDays(1));

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
    }

    @Test
    void placeBid_ShouldAccept_WhenBidIsHigher() {
        BidRequest request = new BidRequest();
        request.setAmount(new BigDecimal("200"));

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        BidResponse response = auctionService.placeBid(1L, request, "testuser");

        assertEquals("ACCEPTED", response.getStatus());
        assertEquals(new BigDecimal("200"), response.getCurrentHighestBid());

        verify(auctionItemRepository, times(1)).save(any(AuctionItem.class));
        verify(bidHistoryRepository, times(1)).save(any());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/auctions/1"), any(BidResponse.class));
    }

    @Test
    void placeBid_ShouldReject_WhenBidIsLower() {
        BidRequest request = new BidRequest();
        request.setAmount(new BigDecimal("100"));

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item));

        BidResponse response = auctionService.placeBid(1L, request, "testuser");

        assertEquals("REJECTED", response.getStatus());
        verify(auctionItemRepository, never()).save(any(AuctionItem.class));
    }

    @Test
    void placeBid_ShouldReject_WhenAuctionHasEnded() {
        item.setEndTime(LocalDateTime.now().minusDays(1));
        
        BidRequest request = new BidRequest();
        request.setAmount(new BigDecimal("200"));

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item));

        BidResponse response = auctionService.placeBid(1L, request, "testuser");

        assertEquals("REJECTED", response.getStatus());
        assertEquals("Auction has ended", response.getMessage());
    }
}
