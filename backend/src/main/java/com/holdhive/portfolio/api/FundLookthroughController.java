package com.holdhive.portfolio.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.holdhive.portfolio.application.FundLookthrough;
import com.holdhive.portfolio.application.FundLookthroughService;

@RestController
@RequestMapping("/api/v1/funds")
public class FundLookthroughController {

    private final FundLookthroughService fundLookthroughService;

    public FundLookthroughController(FundLookthroughService fundLookthroughService) {
        this.fundLookthroughService = fundLookthroughService;
    }

    @GetMapping("/{instrumentId}/lookthrough")
    public FundLookthrough lookthrough(@PathVariable Long instrumentId) {
        return fundLookthroughService.getLookthrough(instrumentId);
    }
}
