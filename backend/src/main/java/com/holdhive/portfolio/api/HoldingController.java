package com.holdhive.portfolio.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.holdhive.portfolio.api.dto.CreateHoldingRequest;
import com.holdhive.portfolio.api.dto.HoldingListResponse;
import com.holdhive.portfolio.api.dto.HoldingMapper;
import com.holdhive.portfolio.api.dto.HoldingResponse;
import com.holdhive.portfolio.api.dto.UpdateHoldingRequest;
import com.holdhive.portfolio.application.HoldingCommandService;
import com.holdhive.portfolio.application.HoldingQueryService;
import com.holdhive.pricing.application.PriceMode;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/holdings")
public class HoldingController {

    private final HoldingCommandService holdingCommandService;
    private final HoldingQueryService holdingQueryService;

    public HoldingController(
        HoldingCommandService holdingCommandService,
        HoldingQueryService holdingQueryService
    ) {
        this.holdingCommandService = holdingCommandService;
        this.holdingQueryService = holdingQueryService;
    }

    @GetMapping
    public HoldingListResponse holdings(
        @RequestParam(defaultValue = "ticker,asc") String sort,
        @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode
    ) {
        return HoldingMapper.toResponse(holdingQueryService.listHoldings(sort, priceMode));
    }

    @GetMapping("/{holdingId}")
    public HoldingResponse holding(
        @PathVariable Long holdingId,
        @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode
    ) {
        return HoldingMapper.toResponse(holdingQueryService.getHolding(holdingId, priceMode));
    }

    @PostMapping
    public ResponseEntity<HoldingResponse> createHolding(
        @Valid @RequestBody CreateHoldingRequest request
    ) {
        Long holdingId = holdingCommandService.createHolding(request.toCommand());
        HoldingResponse response = HoldingMapper.toResponse(
            holdingQueryService.getHolding(holdingId, PriceMode.BEST_AVAILABLE)
        );
        return ResponseEntity
            .created(URI.create("/api/v1/holdings/" + holdingId))
            .body(response);
    }

    @PatchMapping("/{holdingId}")
    public HoldingResponse updateHolding(
        @PathVariable Long holdingId,
        @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode,
        @Valid @RequestBody UpdateHoldingRequest request
    ) {
        Long updatedHoldingId = holdingCommandService.updateHolding(request.toCommand(holdingId));
        return HoldingMapper.toResponse(
            holdingQueryService.getHolding(updatedHoldingId, priceMode)
        );
    }

    @DeleteMapping("/{holdingId}")
    public ResponseEntity<Void> deleteHolding(@PathVariable Long holdingId) {
        holdingCommandService.deleteHolding(holdingId);
        return ResponseEntity.noContent().build();
    }
}
