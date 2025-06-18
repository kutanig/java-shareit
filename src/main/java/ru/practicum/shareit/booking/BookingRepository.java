package ru.practicum.shareit.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByBookerId(Long bookerId, Pageable pageable);

    Page<Booking> findByBookerIdAndStartBeforeAndEndAfter(
            Long bookerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Booking> findByBookerIdAndEndBefore(
            Long bookerId, LocalDateTime end, Pageable pageable);

    Page<Booking> findByBookerIdAndStartAfter(
            Long bookerId, LocalDateTime start, Pageable pageable);

    Page<Booking> findByBookerIdAndStatus(
            Long bookerId, BookingStatus status, Pageable pageable);

    Page<Booking> findByItemOwnerId(Long ownerId, Pageable pageable);

    Page<Booking> findByItemOwnerIdAndStartBeforeAndEndAfter(
            Long ownerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Booking> findByItemOwnerIdAndEndBefore(
            Long ownerId, LocalDateTime end, Pageable pageable);

    Page<Booking> findByItemOwnerIdAndStartAfter(
            Long ownerId, LocalDateTime start, Pageable pageable);

    Page<Booking> findByItemOwnerIdAndStatus(
            Long ownerId, BookingStatus status, Pageable pageable);

    List<Booking> findByItemIdAndBookerIdAndStatusAndEndBefore(
            Long itemId, Long bookerId, BookingStatus status, LocalDateTime end);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.start < :now " +
            "ORDER BY b.start DESC")
    List<Booking> findLastBookingForItem(
            @Param("itemId") Long itemId,
            @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND b.start > :now " +
            "ORDER BY b.start ASC")
    List<Booking> findNextBookingForItem(
            @Param("itemId") Long itemId,
            @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.booker.id = :userId " +
            "AND b.status = 'APPROVED' " +
            "AND b.end < CURRENT_TIMESTAMP")
    List<Booking> findCompletedBookingsForComment(@Param("itemId") Long itemId,
                                                  @Param("userId") Long userId);
}
