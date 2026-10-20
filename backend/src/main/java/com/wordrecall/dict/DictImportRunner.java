package com.wordrecall.dict;

import com.wordrecall.service.DictImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DictImportRunner implements CommandLineRunner {

    private final DictImportService dictImportService;

    @Override
    public void run(String... args) throws Exception {
        dictImportService.importIfEmpty();
        log.info("dictionary import check finished");
    }
}
