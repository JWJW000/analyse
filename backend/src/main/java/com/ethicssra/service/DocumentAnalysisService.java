package com.ethicssra.service;

import com.ethicssra.dto.DocumentAnalysisDto;
import com.ethicssra.dto.IntegrityCheckRequest;
import org.springframework.stereotype.Service;

@Service
public class DocumentAnalysisService {

    private final LanguageAnalysisService languageAnalysisService;
    private final LogicalConsistencyService logicalConsistencyService;

    public DocumentAnalysisService(
            LanguageAnalysisService languageAnalysisService,
            LogicalConsistencyService logicalConsistencyService
    ) {
        this.languageAnalysisService = languageAnalysisService;
        this.logicalConsistencyService = logicalConsistencyService;
    }

    public DocumentAnalysisDto analyze(IntegrityCheckRequest req) {
        var lang = languageAnalysisService.analyze(req.title(), req.textContent());
        var logic = logicalConsistencyService.check(
                req.title(),
                req.textContent(),
                req.specWizardJson(),
                req.embeddedModules(),
                lang
        );
        return new DocumentAnalysisDto(lang, logic);
    }
}
