//package com.acc.somsomparty.domain.Reservation.service;
//
//import com.acc.somsomparty.domain.Reservation.converter.ReservationConverter;
//import com.acc.somsomparty.domain.Reservation.dto.ReservationResponseDTO;
//import com.acc.somsomparty.domain.Reservation.entity.Reservation;
//import com.acc.somsomparty.domain.Reservation.repository.ReservationRepository;
//import com.acc.somsomparty.global.exception.CustomException;
//import com.acc.somsomparty.global.exception.error.ErrorCode;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class ReservationQueryServiceImpl implements ReservationQueryService {
//    private final ReservationRepository reservationRepository;
//
//    @Override
//    public ReservationResponseDTO.ReservationPreViewListDTO getReservationList(Long userId, Long lastId, int limit) {
//        List<Reservation> result;
//        PageRequest pageRequest = PageRequest.of(0, limit + 1);
//        if (lastId.equals(0L)) {
//            result = reservationRepository.findAllByUserIdOrderByCreatedAtDesc(1L, pageRequest).getContent();
//        }
//        else {
//            Reservation reservation = reservationRepository.findById(lastId).orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_NOT_FOUND));
//            result = reservationRepository.findByUserIdAndCreatedAtLessThanOrderByCreatedAtDesc(1L, reservation.getCreatedAt(), pageRequest).getContent();
//        }
//        return generateReservationPreviewListDTO(result, limit);
//    }
//
//    private ReservationResponseDTO.ReservationPreViewListDTO generateReservationPreviewListDTO(List<Reservation> reservations, int limit) {
//        boolean hasNext = reservations.size() > limit;
//        Long lastId = null;
//        if (hasNext) {
//            reservations = reservations.subList(0, reservations.size() - 1); // 마지막 항목 제외
//            lastId = reservations.get(reservations.size() - 1).getId(); // 마지막 항목의 ID를 커서로 설정
//        }
//        List<ReservationResponseDTO.ReservationPreViewDTO> list = reservations
//                .stream()
//                .map(ReservationConverter::reservationPreViewDTO)
//                .collect(Collectors.toList());
//
//        return ReservationConverter.reservationPreViewListDTO(list, hasNext, lastId);
//    }
//}
