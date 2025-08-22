package com.acc.somsomparty.domain.Festival.controller;

import com.acc.somsomparty.domain.Festival.converter.FestivalConverter;
import com.acc.somsomparty.domain.Festival.dto.FestivalRequestDTO;
import com.acc.somsomparty.domain.Festival.dto.FestivalResponseDTO;
import com.acc.somsomparty.domain.Festival.entity.Festival;

import com.acc.somsomparty.domain.Festival.service.FestivalQueryService;
import com.acc.somsomparty.domain.Queue.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/festivals")
@RequiredArgsConstructor
public class FestivalController {
    private final FestivalQueryService festivalQueryService;
    private final SlotService slotService;

    @PostMapping("/create")
    @Operation(summary = "축제 생성", description = "축제를 생성합니다.")
    public ResponseEntity<Festival> createFestival(FestivalRequestDTO festivalRequestDTO) {
        Festival festival = festivalQueryService.save(festivalRequestDTO);
        return new ResponseEntity<>(festival, HttpStatus.CREATED);
    }

    @GetMapping("")
    public ResponseEntity<FestivalResponseDTO.FestivalPreViewListDTO> getFestivalList(@RequestParam(defaultValue = "0") Long lastId, @RequestParam(defaultValue = "10") int limit) {
        FestivalResponseDTO.FestivalPreViewListDTO festivalPage = festivalQueryService.getFestivalList(lastId, limit);
        return new ResponseEntity<>(festivalPage, HttpStatus.OK);
    }

    @GetMapping("/{festivalId}")
    public ResponseEntity<FestivalResponseDTO.FestivalPreViewDTO> getFestival(@PathVariable(name = "festivalId") Long festivalId) {
        Festival festival = festivalQueryService.getFestival(festivalId);
        return new ResponseEntity<>(FestivalConverter.festivalPreViewDTO(festival), HttpStatus.OK);
    }

    @Operation(summary = "축제 검색",     description = "축제 이름 또는 설명에서 해당 키워드가 포함된 축제들을 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<FestivalResponseDTO.FestivalPreViewListDTO> searchFestival(@RequestParam(defaultValue = "0") Long lastId,
                                                                                     @RequestParam(defaultValue = "10") int limit,
                                                                                     @RequestParam(name = "keyword") String keyword) {
        return ResponseEntity.ok(festivalQueryService.searchFestival(lastId, limit, keyword));
    }

    @PostMapping("/{festivalId}/slots")
    @Operation(summary = "슬롯 초기화", description = "축제 별 슬롯을 초기화합니다.")
    public ResponseEntity<String> setMaxSlot(@PathVariable String festivalId, @RequestParam int maxSlot) {
        slotService.initializeSlot(festivalId, maxSlot).subscribe();
        return ResponseEntity.ok("슬롯 초기화 성공");
    }
}
