package com.example.auction.controller;

import com.example.auction.dto.BidRequest;
import com.example.auction.dto.BidResponse;
import com.example.auction.model.AuctionItem;
import com.example.auction.service.AuctionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping
    public ResponseEntity<List<AuctionItem>> getAllAuctions() {
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionItem> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuctionById(id));
    }

    @PostMapping("/{id}/bid")
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long id,
            @Valid @RequestBody BidRequest bidRequest,
            Authentication authentication) {

        // authentication.getName() gives us the username from the JWT
        // This is the Spring equivalent of req.user.username in Express
        String username = authentication.getName();
        BidResponse response = auctionService.placeBid(id, bidRequest, username);

        if ("REJECTED".equals(response.getStatus())) {
            return ResponseEntity.status(409).body(response); // 409 Conflict
        }
        return ResponseEntity.ok(response);
    }
}
