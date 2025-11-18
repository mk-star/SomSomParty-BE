package com.acc.somsomparty.domain.Festival.service;

import com.acc.somsomparty.domain.Festival.converter.FestivalConverter;
import com.acc.somsomparty.domain.Festival.dto.FestivalRequestDTO;
import com.acc.somsomparty.domain.Festival.dto.FestivalResponseDTO;
import com.acc.somsomparty.domain.Festival.entity.Festival;
import com.acc.somsomparty.domain.Festival.repository.FestivalRepository;
import com.acc.somsomparty.domain.Ticket.entity.Ticket;
import com.acc.somsomparty.global.exception.CustomException;
import com.acc.somsomparty.global.exception.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class FestivalQueryServiceImpl implements FestivalQueryService {
    private final FestivalRepository festivalRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public FestivalResponseDTO.FestivalPreViewListDTO getFestivalList(Long lastId, int limit) {
        List<Festival> result;
        PageRequest pageRequest = PageRequest.of(0, limit + 1);

        if (lastId.equals(0L)) {
            // 첫 페이지 조회
            result = festivalRepository.findAllByOrderByCreatedAtDesc(pageRequest).getContent();
        }
        else {
            // 두 번째 페이지부터
            Festival lastFestival = festivalRepository.findById(lastId)
                    .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_NOT_FOUND));

            result = festivalRepository.findNextPage(lastFestival.getCreatedAt(), lastId, pageRequest);
        }
        return generateFestivalPreviewListDTO(result, limit);
    }

    @Override
    public Festival getFestival(Long festivalId) {
        return festivalRepository.findById(festivalId).orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_NOT_FOUND));
    }

    @Override
    public FestivalResponseDTO.FestivalPreViewListDTO searchFestival(Long lastId, int limit, String keyword) {
        // Redis 캐시 키 생성
        String cacheKey = "festival::search::" + keyword.toLowerCase() + "::" + lastId + "::" + limit;

        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            // 캐시된 결과 반환
            return objectMapper.convertValue(cachedResult, FestivalResponseDTO.FestivalPreViewListDTO.class);
        }

        // 캐시에 없으면 DB에서 검색
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Festival> festivals = festivalRepository.searchByKeyword(lastId, keyword.toLowerCase(), pageRequest).getContent();
        FestivalResponseDTO.FestivalPreViewListDTO responseDTO = generateFestivalPreviewListDTO(festivals, limit);

        // Redis에 결과 캐싱 (TTL 10분 설정)
        redisTemplate.opsForValue().set(cacheKey, responseDTO, 5, TimeUnit.MINUTES);

        return responseDTO;
    }

    @Override
    public Festival save(FestivalRequestDTO festivalRequestDTO) {
        // Festival 생성
        Festival festival = new Festival(festivalRequestDTO.getTitle(),festivalRequestDTO.getDescription(),
                festivalRequestDTO.getStartDate(), festivalRequestDTO.getEndDate());

        // startDate부터 endDate까지의 날짜에 대한 Ticket 생성
        LocalDate startDate = festivalRequestDTO.getStartDate();
        LocalDate endDate = festivalRequestDTO.getEndDate();

        // TODO: 기본 총 티켓 수 (필요 시 변경)
        int defaultTotalTickets = 100;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Ticket ticket = Ticket.builder()
                    .festival(festival)
                    .festivalDate(date)
                    .totalTickets(defaultTotalTickets)
                    .leftTickets(defaultTotalTickets)
                    .build();

            festival.getTicketList().add(ticket);
        }

        Festival savedFestival = festivalRepository.save(festival);

        // Redis 캐시 무효화
        String cacheKeyPattern = "festival::search::*";
        redisTemplate.keys(cacheKeyPattern).forEach(redisTemplate::delete);

        return savedFestival;
    }

    private FestivalResponseDTO.FestivalPreViewListDTO generateFestivalPreviewListDTO(List<Festival> festivals, int limit) {
        boolean hasNext = festivals.size() > limit;
        List<Festival> content = hasNext ? festivals.subList(0, limit) : festivals;

        Long lastId = null;
        if (!content.isEmpty()) {
            // content의 마지막 항목 ID를 커서로 설정
            lastId = content.get(content.size() - 1).getId();
        }

        List<FestivalResponseDTO.FestivalPreViewDTO> list = festivals
                .stream()
                .map(FestivalConverter::festivalPreViewDTO)
                .collect(Collectors.toList());

        return FestivalConverter.festivalPreViewListDTO(list, hasNext, lastId);
    }
}
