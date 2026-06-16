# Implementation Plan: MVH Booking Management System

## Overview

This implementation plan converts the MVH Booking Management system design into actionable development tasks. The system is organized into five use cases (UC18-UC22), each implemented in a separate Git branch with full frontend (Tailwind CSS) and backend (Spring Boot) components, comprehensive testing (unit tests, property-based tests with jqwik, and Swagger testing), and proper Git workflow with Pull Requests.

### Implementation Strategy

- **Branch per Use Case**: Each UC has its own feature branch
- **No Direct Main Commits**: All changes go through Pull Requests
- **Frontend-First**: Modern Tailwind CSS responsive UI
- **Backend with Swagger**: All APIs testable in Swagger UI
- **Comprehensive Testing**: Unit tests, property tests (jqwik), integration tests
- **Progressive Integration**: Each UC builds on previous work

### Technology Stack

- **Backend**: Spring Boot 3.x, Spring Security (JWT), JPA/Hibernate, SQL Server, iText PDF
- **Frontend**: HTML5, Tailwind CSS 3.x, Vanilla JavaScript
- **Testing**: JUnit 5, jqwik (property-based testing), MockMvc, Swagger UI
- **Tools**: Maven, Git, Swagger/OpenAPI 3.0

---

## Tasks

### 0. Project Setup and Configuration

- [ ] 0.1 Setup project infrastructure and dependencies
  - Clone repository: https://github.com/hungkizsp/SWP-BookingTour.git
  - Verify Spring Boot project structure and SQL Server connection
  - Add required dependencies to pom.xml: iText for PDF generation, jqwik for property testing
  - Configure Tailwind CSS in frontend (CDN or build process)
  - _Requirements: 9.4, 10.5_

- [ ] 0.2 Configure Swagger/OpenAPI documentation
  - Add Swagger dependencies (springdoc-openapi-starter-webmvc-ui)
  - Configure Swagger UI at /swagger-ui.html
  - Add JWT authentication configuration to Swagger
  - Test Swagger UI access
  - _Requirements: 10.1, 10.4_

- [ ] 0.3 Setup database schema and indexes
  - Create/verify tables: bookings, customers, tours, payments, booking_status_history, refunds
  - Add database indexes: idx_customer_id, idx_status, idx_departure_date
  - Verify entity relationships and foreign keys
  - Test database connectivity
  - _Requirements: 13.2_


---

### UC18: View Booking History (feature/mvh-booking-history)

- [x] 1. Setup UC18 branch and prepare backend foundation
  - Create branch: `git checkout -b feature/mvh-booking-history`
  - Create BookingController with @RestController and @RequestMapping("/api/bookings")
  - Add Swagger annotations (@Tag, @SecurityRequirement)
  - Create placeholder GET /api/bookings endpoint
  - _Requirements: 9.1, 10.1_

- [x] 2. Implement backend data layer for booking history
  - [x] 2.1 Create/update Booking entity with indexes
    - Add @Entity annotations and @Table with indexes (customer_id, status, departure_date)
    - Define relationships: @ManyToOne customer, @ManyToOne tour, @OneToOne payment
    - Add version field for optimistic locking
    - _Requirements: 1.1, 13.2, 7.4_

  - [x] 2.2 Create BookingRepository interface
    - Extend JpaRepository<Booking, Long>
    - Add custom query methods with @Query for filtering and search
    - Add method with JOIN FETCH to prevent N+1 queries
    - _Requirements: 1.1, 1.3, 1.4_

  - [x] 2.3 Create DTOs for booking history
    - Create BookingHistoryDTO with tour name, destination, status, amount, dates
    - Create BookingHistoryResponse with content, pagination, and statistics
    - Create BookingStatistics with totalBookings, totalSpent, status counts
    - _Requirements: 1.1, 1.5_

- [x] 3. Implement booking history service layer
  - [x] 3.1 Create BookingService with business logic
    - Implement getBookingHistory() with pagination, search, and filters
    - Add authorization check: verify customer ID from JWT matches booking owner
    - Implement search across tour name, booking reference, destination
    - Implement filters: status array, date range, price range
    - _Requirements: 1.1, 1.3, 1.4, 6.2, 6.3_

  - [x] 3.2 Implement statistics calculation
    - Create calculateStatistics() method
    - Aggregate totalBookings, totalSpent, counts by status
    - Ensure accurate calculations matching actual data
    - _Requirements: 1.5_


- [x] 4. Implement booking history REST API endpoint
  - [x] 4.1 Complete GET /api/bookings controller method
    - Add @GetMapping with @Operation Swagger annotation
    - Extract authenticated customer ID from SecurityContext/JWT
    - Add query parameters: page, size, search, status[], dateFrom, dateTo, priceMin, priceMax
    - Call BookingService.getBookingHistory()
    - Return ResponseEntity with BookingHistoryResponse
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 6.1_

  - [x] 4.2 Add Swagger documentation for booking history endpoint
    - Document all query parameters with @Parameter
    - Add example request and response with @ApiResponse
    - Document 200 OK, 401 Unauthorized, 500 Internal Server Error responses
    - Test endpoint in Swagger UI with sample data
    - _Requirements: 10.1, 10.2, 10.3, 10.5_

- [ ] 5. Create responsive booking history frontend with Tailwind CSS
  - [ ] 5.1 Create booking history page structure
    - Create bookings.html or integrate into existing frontend structure
    - Add page header with "My Bookings" title and user avatar placeholder
    - Setup responsive grid layout: mobile (single column), tablet (2 columns), desktop (full layout)
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 5.2 Implement statistics cards component
    - Create 4 gradient cards: Total Bookings, Confirmed, Pending, Total Spent
    - Use grid: grid-cols-1 md:grid-cols-2 lg:grid-cols-4
    - Add hover scale animation: transform hover:scale-105
    - Include icons for each statistic (calendar, checkmark, clock, dollar)
    - _Requirements: 1.5, 8.1, 8.2, 8.3_

  - [ ] 5.3 Implement search and filter UI
    - Create search input with left icon: absolute positioning for icon
    - Add filter dropdown pills: Status, Date Range, Price Range
    - Implement "Clear all" button (shown when filters active)
    - Use Tailwind responsive classes for mobile/tablet/desktop
    - _Requirements: 1.3, 1.4, 8.6_

  - [ ] 5.4 Implement booking card component
    - Create card with tour image (1/3 width), details (2/3 width) on desktop
    - Stack image on top for mobile: flex-col on mobile, md:flex-row on desktop
    - Add status badge overlay on image
    - Include booking details: tour name, destination, departure date, duration, guests, price
    - Add "View Details" button with hover animation
    - Use shadow-md hover:shadow-xl transition
    - _Requirements: 1.1, 8.1, 8.2, 8.3, 8.6_

  - [ ] 5.5 Implement loading and empty states
    - Create loading skeleton with animate-pulse
    - Create empty state with icon, message, "Clear Filters" button
    - Show appropriate state based on data/loading status
    - _Requirements: 1.7_

  - [ ] 5.6 Implement pagination component
    - Create mobile pagination: Previous/Next buttons only
    - Create desktop pagination: page numbers with current page highlighted
    - Show "Showing X to Y of Z results" text
    - Add click handlers for page navigation
    - _Requirements: 1.2, 8.1, 8.2, 8.3_


- [ ] 6. Implement frontend API integration for booking history
  - [ ] 6.1 Create API client with authentication
    - Create bookingApi.js with fetch wrapper
    - Add JWT token injection: Authorization: Bearer <token> header
    - Implement getBookingHistory(filters, page, size) function
    - Add error handling and retry logic for network failures
    - _Requirements: 6.1, 7.3_

  - [ ] 6.2 Implement search functionality
    - Add input event listener with 300ms debounce
    - Call API on search text change
    - Display loading state during search
    - Update booking list with results
    - _Requirements: 1.3, 13.3_

  - [ ] 6.3 Implement filter functionality
    - Add change listeners to filter dropdowns
    - Combine all active filters into single API request
    - Update URL query params for bookmarkable state (optional)
    - Clear filters button resets all filters and reloads
    - _Requirements: 1.4_

  - [ ] 6.4 Implement pagination navigation
    - Add click handlers to Previous/Next buttons
    - Add click handlers to page number buttons
    - Update current page and fetch new data
    - Scroll to top on page change
    - _Requirements: 1.2_

  - [ ] 6.5 Display statistics from API response
    - Parse statistics object from API response
    - Update statistics cards with real data
    - Format currency values (e.g., $12,450.00)
    - _Requirements: 1.5_

- [ ] 7. Testing for UC18
  - [ ]* 7.1 Write property test for Customer Booking Isolation (Property 5)
    - **Property 5: Customer Booking Isolation**
    - Use jqwik with @Property annotation
    - Generate arbitrary customers and booking lists
    - Verify all returned bookings belong exclusively to authenticated customer
    - **Validates: Requirements 1.1**

  - [ ]* 7.2 Write property test for Pagination Correctness (Property 6)
    - **Property 6: Pagination Correctness**
    - Generate arbitrary page numbers and page sizes
    - Verify returned items match page size (or fewer on last page)
    - Verify total count accuracy
    - **Validates: Requirements 1.2**

  - [ ]* 7.3 Write property test for Search Result Matching (Property 7)
    - **Property 7: Search Result Matching**
    - Generate arbitrary booking lists and search terms
    - Verify all results contain search term in tour name, booking reference, or destination
    - **Validates: Requirements 1.3**

  - [ ]* 7.4 Write property test for Filter Result Compliance (Property 8)
    - **Property 8: Filter Result Compliance**
    - Generate arbitrary filter combinations (status, date range, price range)
    - Verify all returned bookings match ALL active filter criteria
    - **Validates: Requirements 1.4**

  - [ ]* 7.5 Write property test for Statistics Calculation Accuracy (Property 9)
    - **Property 9: Statistics Calculation Accuracy**
    - Generate arbitrary booking sets
    - Calculate expected statistics manually
    - Verify calculated statistics match actual aggregated values
    - **Validates: Requirements 1.5**

  - [ ]* 7.6 Write unit tests for booking history service
    - Test getBookingHistory with various filter combinations
    - Test empty result set returns empty list with correct pagination
    - Test search with special characters and empty strings
    - Test authorization check rejects wrong customer ID

  - [ ]* 7.7 Write integration tests for booking history API
    - Test GET /api/bookings returns 200 with valid JWT
    - Test GET /api/bookings returns 401 without JWT
    - Test pagination parameters work correctly
    - Test all filters apply correctly
    - Use Swagger UI to manually test all scenarios

  - [ ]* 7.8 Test responsive UI across devices
    - Test mobile view (<768px): single column, stacked cards, touch-friendly buttons
    - Test tablet view (768-1024px): 2-column grid, side-by-side filters
    - Test desktop view (>1024px): 4-column statistics, full-width search
    - Verify 44x44px minimum touch targets on mobile


- [ ] 8. Git workflow and PR for UC18
  - Commit all changes to feature/mvh-booking-history branch
  - Push branch: `git push -u origin feature/mvh-booking-history`
  - Create Pull Request for code review (do NOT merge to main)
  - Add PR description: summary of changes, testing performed, screenshots
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 9. Checkpoint - UC18 Complete
  - Ensure all tests pass (unit tests, property tests, Swagger tests)
  - Verify booking history page works on mobile, tablet, desktop
  - Verify all filters and search work correctly
  - Ask user if questions arise before proceeding to UC19

---

### UC19: View Booking Detail (feature/mvh-booking-detail)

- [ ] 10. Setup UC19 branch and extend backend
  - Create branch from main: `git checkout -b feature/mvh-booking-detail`
  - Verify BookingController exists (from UC18 or create new)
  - Create placeholder GET /api/bookings/{id} endpoint
  - _Requirements: 9.1, 10.1_

- [ ] 11. Implement backend for booking detail
  - [ ] 11.1 Create BookingDetailDTO with comprehensive data
    - Include booking reference, status, created timestamp
    - Nested DTOs: TourInfoDTO, CustomerInfoDTO, PaymentInfoDTO
    - Include List<StatusHistoryDTO> for status timeline
    - Include RefundInfoDTO (nullable) for refund information
    - Include cancellation reason and timestamp (nullable)
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ] 11.2 Extend BookingRepository with detail query
    - Add findByIdWithDetails using @Query with JOIN FETCH
    - Fetch tour, customer, payment, statusHistory, refund in single query
    - Prevent N+1 query problem
    - _Requirements: 2.1_

  - [ ] 11.3 Implement getBookingDetail in BookingService
    - Fetch booking by ID with all relationships
    - Check authorization: verify customer ID matches authenticated user
    - Throw ForbiddenException if authorization fails
    - Throw NotFoundException if booking doesn't exist
    - Map entity to BookingDetailDTO
    - _Requirements: 2.1, 2.7, 6.3, 6.4_

  - [ ] 11.4 Sort status history chronologically
    - Order status history by timestamp DESC (most recent first)
    - Ensure ordering in repository query with @OrderBy or manual sorting
    - _Requirements: 2.5, 11.4_


- [ ] 12. Implement booking detail REST API endpoint
  - [ ] 12.1 Complete GET /api/bookings/{id} controller method
    - Add @GetMapping("/{bookingId}") with @PathVariable
    - Add Swagger @Operation annotation with summary and description
    - Extract authenticated customer ID from SecurityContext
    - Call BookingService.getBookingDetail(bookingId, customerId)
    - Return ResponseEntity<BookingDetailDTO>
    - _Requirements: 2.1, 6.1, 6.2_

  - [ ] 12.2 Add exception handlers for booking detail errors
    - Handle ForbiddenException → 403 Forbidden response
    - Handle NotFoundException → 404 Not Found response
    - Return consistent error response structure with timestamp, status, message, path
    - _Requirements: 2.7, 7.2_

  - [ ] 12.3 Add Swagger documentation for booking detail endpoint
    - Document path parameter: bookingId
    - Add example responses: 200 OK, 401 Unauthorized, 403 Forbidden, 404 Not Found
    - Include complete BookingDetailDTO example in response
    - Test endpoint in Swagger UI with sample booking IDs
    - _Requirements: 10.1, 10.2, 10.3, 10.5_

- [ ] 13. Create responsive booking detail frontend with Tailwind CSS
  - [ ] 13.1 Create booking detail page structure
    - Create booking-detail.html or integrate into frontend routing
    - Add breadcrumb: "My Bookings > Booking Details"
    - Create hero section with tour image, title, status badge
    - Setup 2-column layout for desktop, stacked for mobile
    - _Requirements: 8.4, 8.5_

  - [ ] 13.2 Implement hero section component
    - Full-width hero with background image and gradient overlay
    - Display tour name, location, date range, booking reference
    - Show status badge with appropriate color (success/warning/danger)
    - Responsive height: h-64 mobile, h-80 desktop
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 13.3 Implement tour information card
    - Display tour description, departure date, duration, destination, participants
    - Use grid layout for tour details: 1 column mobile, 2 columns desktop
    - Add icon for each detail field (calendar, clock, location, users)
    - Show included services list with checkmark icons
    - _Requirements: 2.2_

  - [ ] 13.4 Implement customer information card
    - Display customer avatar with initials
    - Show customer name, email, phone, number of participants
    - Use flex layout with icons for each field
    - _Requirements: 2.3_

  - [ ] 13.5 Implement status timeline component
    - Display vertical timeline with left border line
    - Show all status transitions chronologically (most recent first)
    - Current status with pulsing animation and highlighted background
    - Past statuses with gray styling
    - Include status name, description, and timestamp
    - _Requirements: 2.5, 11.4_

  - [ ] 13.6 Implement payment details card
    - Display payment status badge (success if completed)
    - Show transaction reference, payment method, payment date
    - Display itemized breakdown: subtotal, service fee, tax
    - Show total amount prominently
    - _Requirements: 2.4_

  - [ ] 13.7 Implement booking summary sidebar (desktop)
    - Create sticky sidebar with top-6 positioning
    - Display total price in gradient card
    - Show booking status and payment status
    - Add quick action buttons: Download Invoice, Cancel Booking
    - Add support card with "Contact Support" button
    - Hide sidebar on mobile, show at bottom instead
    - _Requirements: 8.2, 8.3, 8.5_


- [ ] 14. Implement frontend API integration for booking detail
  - [ ] 14.1 Create getBookingDetail API function
    - Add getBookingDetail(bookingId) to bookingApi.js
    - Include JWT token in Authorization header
    - Handle errors: 401 redirect to login, 403 show error, 404 show not found
    - Return parsed BookingDetailDTO
    - _Requirements: 6.1, 6.2, 7.2_

  - [ ] 14.2 Implement page data loading
    - Extract bookingId from URL parameter
    - Show loading skeleton while fetching data
    - Call getBookingDetail(bookingId) on page load
    - Populate all page sections with fetched data
    - Handle loading state, error state, success state
    - _Requirements: 2.1_

  - [ ] 14.3 Implement conditional action button display
    - Show "Download Invoice" only if status is CONFIRMED or COMPLETED
    - Show "Cancel Booking" only if status is CONFIRMED or PENDING
    - Show "Request Refund" only if status is CANCELLED and no refund exists
    - Disable buttons and show appropriate tooltips when conditions not met
    - _Requirements: 3.1, 4.1, 4.2, 5.1, 5.2_

- [ ] 15. Testing for UC19
  - [ ]* 15.1 Write property test for Authorization Enforcement (Property 2)
    - **Property 2: Authorization Enforcement**
    - Generate arbitrary customers and bookings
    - Verify customer can only access their own bookings
    - Verify accessing other customer's booking returns 403
    - **Validates: Requirements 2.7, 6.3, 6.4**

  - [ ]* 15.2 Write property test for Booking Detail Completeness (Property 3)
    - **Property 3: Booking Detail Completeness**
    - Generate arbitrary bookings with all required fields
    - Verify detail view includes all required fields: booking reference, tour info, customer info, payment info
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**

  - [ ]* 15.3 Write property test for Status History Ordering (Property 4)
    - **Property 4: Status History Chronological Ordering**
    - Generate bookings with multiple status transitions
    - Verify status history is ordered chronologically (most recent first)
    - **Validates: Requirements 2.5, 11.4**

  - [ ]* 15.4 Write unit tests for booking detail service
    - Test getBookingDetail with valid booking ID returns complete data
    - Test getBookingDetail with invalid booking ID throws NotFoundException
    - Test getBookingDetail with wrong customer ID throws ForbiddenException
    - Test status history ordering is correct

  - [ ]* 15.5 Write integration tests for booking detail API
    - Test GET /api/bookings/{id} returns 200 with valid JWT and owned booking
    - Test GET /api/bookings/{id} returns 403 when accessing other customer's booking
    - Test GET /api/bookings/{id} returns 404 with non-existent booking ID
    - Test GET /api/bookings/{id} returns 401 without JWT
    - Use Swagger UI to test all scenarios

  - [ ]* 15.6 Test responsive UI for booking detail
    - Test mobile view: hero section, stacked cards, action buttons at bottom
    - Test tablet view: 2-column layout for tour details
    - Test desktop view: 2-column layout with sticky sidebar
    - Verify all content is readable and accessible

- [ ] 16. Git workflow and PR for UC19
  - Commit all changes to feature/mvh-booking-detail branch
  - Push branch: `git push -u origin feature/mvh-booking-detail`
  - Create Pull Request for code review (do NOT merge to main)
  - Add PR description with screenshots of booking detail page
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 17. Checkpoint - UC19 Complete
  - Ensure all tests pass
  - Verify booking detail page displays all information correctly
  - Verify responsive design works on all devices
  - Ask user if questions arise before proceeding to UC20

---

### UC20: Cancel Booking (feature/mvh-cancel-booking)

- [ ] 18. Setup UC20 branch and extend backend
  - Create branch from main: `git checkout -b feature/mvh-cancel-booking`
  - Verify BookingController and BookingService exist
  - Create placeholder POST /api/bookings/{id}/cancel endpoint
  - _Requirements: 9.1, 10.1_


- [ ] 19. Implement backend cancellation service
  - [ ] 19.1 Create CancelBookingRequest DTO
    - Add reason field (required, @NotBlank)
    - Add additionalDetails field (optional)
    - Add validation annotations
    - _Requirements: 3.2_

  - [ ] 19.2 Create CancelBookingResponse DTO
    - Include success flag, message, booking reference
    - Include cancellation timestamp
    - Include refund eligibility information (optional)
    - _Requirements: 3.7_

  - [ ] 19.3 Implement cancelBooking business logic in BookingService
    - Fetch booking by ID with authorization check
    - Validate booking status: must be CONFIRMED or PENDING
    - Validate departure date: must be in future (not started)
    - Update booking status to CANCELLED
    - Record cancellation reason and timestamp
    - Create status history entry for cancellation
    - Return success response
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 11.3_

  - [ ] 19.4 Add custom exceptions for cancellation
    - Create BusinessRuleException for invalid state (already cancelled, completed, started)
    - Throw with specific error messages for each violation
    - _Requirements: 3.3, 3.4, 7.2_

- [ ] 20. Implement cancel booking REST API endpoint
  - [ ] 20.1 Complete POST /api/bookings/{id}/cancel controller method
    - Add @PostMapping("/{bookingId}/cancel")
    - Add Swagger @Operation annotation
    - Add @RequestBody with @Valid CancelBookingRequest
    - Extract authenticated customer ID from SecurityContext
    - Call BookingService.cancelBooking()
    - Return 200 OK with CancelBookingResponse
    - _Requirements: 3.1, 3.10, 6.1_

  - [ ] 20.2 Add exception handler for cancellation errors
    - Handle BusinessRuleException → 400 Bad Request with specific message
    - Handle ForbiddenException → 403 Forbidden
    - Return error response with timestamp, status, message, path
    - _Requirements: 3.8, 7.2_

  - [ ] 20.3 Add Swagger documentation for cancel endpoint
    - Document path parameter and request body
    - Add example responses: 200 OK, 400 Bad Request (multiple scenarios), 403 Forbidden, 404 Not Found
    - Include example CancelBookingRequest and CancelBookingResponse
    - Test endpoint in Swagger UI
    - _Requirements: 10.1, 10.2, 10.3, 10.5_

- [ ] 21. Create cancel booking modal component with Tailwind CSS
  - [ ] 21.1 Implement modal backdrop and panel
    - Create fixed inset-0 backdrop with bg-gray-900/75 and backdrop-blur
    - Center modal panel with rounded-2xl and shadow-2xl
    - Add slide-up animation (animate-slide-up)
    - Make modal responsive: full-width on mobile, max-w-lg on desktop
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 21.2 Implement modal header with warning
    - Create gradient danger header: from-danger-500 to-danger-600
    - Add warning icon in white rounded background
    - Display "Cancel Booking" title
    - _Requirements: 3.2_

  - [ ] 21.3 Implement modal content
    - Display warning message about cancellation being irreversible
    - Show booking summary card with tour name, date, amount
    - Create radio button group for cancellation reasons: Schedule Conflict, Found Better Option, Personal Reasons, Other
    - Add optional textarea for additional details
    - Display cancellation policy note with refund conditions
    - _Requirements: 3.2_

  - [ ] 21.4 Implement modal actions
    - Add "Keep Booking" button (secondary style)
    - Add "Confirm Cancellation" button (danger style)
    - Responsive button layout: stacked on mobile, side-by-side on desktop
    - Disable "Confirm" button if no reason selected
    - _Requirements: 3.2_


- [ ] 22. Implement frontend cancel booking functionality
  - [ ] 22.1 Create cancelBooking API function
    - Add cancelBooking(bookingId, request) to bookingApi.js
    - POST to /api/bookings/{id}/cancel with request body
    - Include JWT token in Authorization header
    - Handle errors: 400 show specific error, 403/404 show appropriate messages
    - _Requirements: 3.10, 6.1_

  - [ ] 22.2 Implement modal open/close logic
    - Add click handler to "Cancel Booking" button in detail page
    - Show modal with fade-in animation
    - Add close handlers: backdrop click, X button, "Keep Booking" button
    - Hide modal on close with fade-out animation
    - _Requirements: 3.1, 3.2_

  - [ ] 22.3 Implement cancellation form submission
    - Validate reason is selected before allowing submission
    - Show loading state on "Confirm Cancellation" button during API call
    - Call cancelBooking API with selected reason and additional details
    - On success: close modal, show success toast notification, refresh booking detail
    - On error: show error toast with specific error message from API
    - _Requirements: 3.2, 3.7, 3.8_

- [ ] 23. Implement success and error notifications
  - [ ] 23.1 Create toast notification component
    - Implement success toast with green border and checkmark icon
    - Implement error toast with red border and X icon
    - Position toast: fixed top-4 right-4 with slide-in animation
    - Auto-dismiss after 5 seconds with fade-out
    - _Requirements: 3.7, 3.8_

  - [ ] 23.2 Display success notification on cancellation
    - Show "Booking cancelled successfully" message
    - Include booking reference in notification
    - Update booking detail page to reflect cancelled status
    - _Requirements: 3.7_

  - [ ] 23.3 Display error notifications for cancellation failures
    - Parse error message from API response
    - Show specific error for each failure scenario: already started, already cancelled, already completed
    - Display generic error for unexpected failures
    - _Requirements: 3.8, 7.2_

- [ ] 24. Testing for UC20
  - [ ]* 24.1 Write property test for Cancellation Business Rule - Started Tours (Property 10)
    - **Property 10: Cancellation Business Rule - Started Tours**
    - Generate bookings with past departure dates
    - Verify all cancellation attempts are rejected with appropriate error
    - **Validates: Requirements 3.3**

  - [ ]* 24.2 Write property test for Cancellation Business Rule - Terminal States (Property 11)
    - **Property 11: Cancellation Business Rule - Terminal States**
    - Generate bookings with CANCELLED or COMPLETED status
    - Verify cancellation requests are rejected
    - **Validates: Requirements 3.4**

  - [ ]* 24.3 Write property test for Cancellation State Transition (Property 12)
    - **Property 12: Cancellation State Transition**
    - Generate valid cancellable bookings (CONFIRMED/PENDING, future date)
    - Verify successful cancellation changes status to CANCELLED
    - **Validates: Requirements 3.5**

  - [ ]* 24.4 Write property test for Cancellation Metadata Recording (Property 13)
    - **Property 13: Cancellation Metadata Recording**
    - Generate arbitrary cancellation reasons
    - Verify cancellation reason and timestamp are populated after cancellation
    - **Validates: Requirements 3.6**

  - [ ]* 24.5 Write unit tests for cancellation service
    - Test cancelBooking with valid booking succeeds
    - Test cancelBooking with past departure date throws exception
    - Test cancelBooking with CANCELLED status throws exception
    - Test cancelBooking with COMPLETED status throws exception
    - Test cancellation creates status history entry

  - [ ]* 24.6 Write integration tests for cancel booking API
    - Test POST /api/bookings/{id}/cancel returns 200 with valid request
    - Test POST /api/bookings/{id}/cancel returns 400 when tour started
    - Test POST /api/bookings/{id}/cancel returns 400 when already cancelled
    - Test POST /api/bookings/{id}/cancel returns 403 for other customer's booking
    - Test POST /api/bookings/{id}/cancel returns 401 without JWT
    - Use Swagger UI to test all scenarios

  - [ ]* 24.7 Test cancel booking modal UI
    - Test modal opens on "Cancel Booking" button click
    - Test modal closes on backdrop click, X button, "Keep Booking" button
    - Test "Confirm" button disabled when no reason selected
    - Test form submission and success notification display
    - Test error notification display for various error scenarios


- [ ] 25. Git workflow and PR for UC20
  - Commit all changes to feature/mvh-cancel-booking branch
  - Push branch: `git push -u origin feature/mvh-cancel-booking`
  - Create Pull Request for code review (do NOT merge to main)
  - Add PR description with screenshots of cancel modal and notifications
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 26. Checkpoint - UC20 Complete
  - Ensure all tests pass
  - Verify cancel booking modal works correctly
  - Test all business rule validations (started tour, invalid status)
  - Verify success and error notifications display properly
  - Ask user if questions arise before proceeding to UC21

---

### UC21: Request Refund (feature/mvh-request-refund)

- [ ] 27. Setup UC21 branch and extend backend
  - Create branch from main: `git checkout -b feature/mvh-request-refund`
  - Create Refund entity if not exists
  - Create RefundRepository extending JpaRepository
  - Create placeholder POST /api/bookings/{id}/refund endpoint
  - _Requirements: 9.1, 10.1_

- [ ] 28. Implement backend refund data layer
  - [ ] 28.1 Create/update Refund entity
    - Add @Entity annotations with table name "refunds"
    - Add fields: id, booking (OneToOne), refundReference, amount, status, bank account details, timestamps
    - Encrypt accountNumber field with @Convert(converter = SensitiveDataConverter.class)
    - Add version field for optimistic locking
    - _Requirements: 4.6, 12.1_

  - [ ] 28.2 Create SensitiveDataConverter for encryption
    - Implement AttributeConverter<String, String>
    - Encrypt account numbers using AES-256 before storing
    - Decrypt when reading from database
    - _Requirements: 4.6_

  - [ ] 28.3 Extend RefundRepository
    - Add findByBookingId(Long bookingId) method
    - Add existsByBookingId(Long bookingId) method for duplicate check
    - _Requirements: 4.9_

- [ ] 29. Implement backend refund service
  - [ ] 29.1 Create RefundRequestDTO
    - Add fields: bankAccountHolder, bankName, accountNumber, routingNumber
    - Add validation: @NotBlank on all fields
    - _Requirements: 4.3, 4.4_

  - [ ] 29.2 Create RefundResponseDTO
    - Include success flag, message, refund reference
    - Include amount, status, expected processing days
    - Include requested timestamp
    - _Requirements: 4.7_

  - [ ] 29.3 Implement requestRefund in RefundService
    - Fetch booking by ID with authorization check
    - Validate booking status is CANCELLED
    - Check if refund already exists (duplicate prevention)
    - Validate all bank account fields are present and not empty
    - Create refund record with status PENDING
    - Link refund to booking, store bank information securely
    - Generate unique refund reference
    - Record requested timestamp
    - Return success response with refund details
    - _Requirements: 4.1, 4.2, 4.4, 4.5, 4.6, 4.9, 4.11, 12.2, 12.3_


- [ ] 30. Implement request refund REST API endpoint
  - [ ] 30.1 Complete POST /api/bookings/{id}/refund controller method
    - Add @PostMapping("/{bookingId}/refund")
    - Add Swagger @Operation annotation
    - Add @RequestBody with @Valid RefundRequestDTO
    - Extract authenticated customer ID from SecurityContext
    - Call RefundService.requestRefund()
    - Return 201 Created with RefundResponseDTO
    - _Requirements: 4.11, 6.1_

  - [ ] 30.2 Add exception handlers for refund errors
    - Handle validation errors → 400 Bad Request with field-specific messages
    - Handle BusinessRuleException (not cancelled, duplicate refund) → 400/409
    - Handle ForbiddenException → 403 Forbidden
    - _Requirements: 4.2, 4.9, 7.2_

  - [ ] 30.3 Add Swagger documentation for refund endpoint
    - Document path parameter and request body with all fields
    - Add example responses: 201 Created, 400 Bad Request, 409 Conflict, 403 Forbidden
    - Include bank account field descriptions and validation rules
    - Test endpoint in Swagger UI with sample data
    - _Requirements: 10.1, 10.2, 10.3, 10.5_

- [ ] 31. Create refund request page with Tailwind CSS
  - [ ] 31.1 Create refund request page structure
    - Create refund-request.html or integrate into routing
    - Add page header: "Request Refund"
    - Display booking reference card with refund amount
    - Create form card with white background and shadow
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 31.2 Implement booking reference card
    - Display booking reference and tour name
    - Show refund amount prominently in large text
    - Use primary gradient background (from-primary-500 to-primary-600)
    - _Requirements: 4.1_

  - [ ] 31.3 Implement bank information form
    - Create form with section title "Bank Account Information"
    - Add input fields: Account Holder Name, Bank Name (dropdown), Account Number, Routing Number
    - Apply Tailwind form styles with focus:ring-2 and focus:ring-primary-500
    - Add asterisks for required fields
    - Use monospace font for account number field
    - _Requirements: 4.3, 4.4_

  - [ ] 31.4 Implement security notice
    - Display blue info card with shield icon
    - Message: "Your Information is Secure - All bank account information is encrypted..."
    - _Requirements: 4.6_

  - [ ] 31.5 Implement refund processing timeline
    - Display 3-step timeline: Submission, Review (1-2 days), Processing (5-7 days)
    - Use numbered circles with connecting lines
    - Gray background card with step descriptions
    - _Requirements: 4.7_

  - [ ] 31.6 Implement terms agreement checkbox
    - Add checkbox with label: "I confirm that the bank account information provided is accurate..."
    - Require checkbox to be checked before form submission
    - _Requirements: 4.3_

  - [ ] 31.7 Implement form actions
    - Add "Cancel" button (secondary style)
    - Add "Submit Refund Request" button (primary style) with paper airplane icon
    - Responsive layout: stacked on mobile, side-by-side on desktop
    - Disable submit button if form invalid or terms not agreed
    - _Requirements: 4.3_

- [ ] 32. Implement refund status tracker component
  - Create hidden status card that appears after submission
  - Display refund reference number
  - Show progress bar with current status
  - Display pending review message with expected timeline
  - Update booking detail page to show refund status if exists
  - _Requirements: 4.8, 12.4_


- [ ] 33. Implement frontend refund request functionality
  - [ ] 33.1 Create requestRefund API function
    - Add requestRefund(bookingId, request) to bookingApi.js
    - POST to /api/bookings/{id}/refund with bank account details
    - Include JWT token in Authorization header
    - Handle errors: 400 validation, 409 duplicate, 403 unauthorized
    - _Requirements: 4.11, 6.1_

  - [ ] 33.2 Implement form validation
    - Validate all required fields are filled before submission
    - Validate account number format (numbers only, appropriate length)
    - Validate terms checkbox is checked
    - Show validation error messages inline
    - Disable submit button when form invalid
    - _Requirements: 4.4, 7.1_

  - [ ] 33.3 Implement form submission
    - Add submit handler to refund form
    - Show loading state on submit button
    - Collect form data: bankAccountHolder, bankName, accountNumber, routingNumber
    - Call requestRefund API
    - On success: show success notification, display refund status tracker
    - On error: show error notification with specific validation messages
    - _Requirements: 4.3, 4.7_

  - [ ] 33.4 Implement conditional "Request Refund" button in booking detail
    - Show "Request Refund" button only if booking status is CANCELLED
    - Hide button if refund already exists
    - Navigate to refund request page with booking ID parameter
    - _Requirements: 4.1, 4.2_

- [ ] 34. Testing for UC21
  - [ ]* 34.1 Write property test for Refund Request Validation (Property 14)
    - **Property 14: Refund Request Validation**
    - Generate refund requests with missing or empty bank account fields
    - Verify all requests with incomplete data are rejected
    - **Validates: Requirements 4.4**

  - [ ]* 34.2 Write property test for Refund Creation with Pending Status (Property 15)
    - **Property 15: Refund Creation with Pending Status**
    - Generate valid refund requests on cancelled bookings
    - Verify new refund record created with status PENDING
    - **Validates: Requirements 4.5, 4.6, 12.2**

  - [ ]* 34.3 Write property test for Duplicate Refund Prevention (Property 16)
    - **Property 16: Duplicate Refund Prevention**
    - Generate bookings that already have refund requests
    - Verify subsequent refund attempts are rejected with appropriate error
    - **Validates: Requirements 4.9**

  - [ ]* 34.4 Write unit tests for refund service
    - Test requestRefund with valid data creates refund successfully
    - Test requestRefund on non-cancelled booking throws exception
    - Test requestRefund with existing refund throws duplicate exception
    - Test requestRefund with missing bank fields throws validation exception
    - Test bank account number is encrypted in database

  - [ ]* 34.5 Write integration tests for refund request API
    - Test POST /api/bookings/{id}/refund returns 201 with valid request
    - Test POST /api/bookings/{id}/refund returns 400 when booking not cancelled
    - Test POST /api/bookings/{id}/refund returns 409 when duplicate refund exists
    - Test POST /api/bookings/{id}/refund returns 400 with missing bank fields
    - Test POST /api/bookings/{id}/refund returns 403 for other customer's booking
    - Test POST /api/bookings/{id}/refund returns 401 without JWT
    - Use Swagger UI to test all scenarios

  - [ ]* 34.6 Test refund request form UI
    - Test form validation for all required fields
    - Test terms checkbox requirement
    - Test form submission success flow
    - Test error messages display for validation failures
    - Test refund status tracker appears after submission
    - Test responsive layout on mobile, tablet, desktop


- [ ] 35. Git workflow and PR for UC21
  - Commit all changes to feature/mvh-request-refund branch
  - Push branch: `git push -u origin feature/mvh-request-refund`
  - Create Pull Request for code review (do NOT merge to main)
  - Add PR description with screenshots of refund request form and status tracker
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 36. Checkpoint - UC21 Complete
  - Ensure all tests pass
  - Verify refund request form validates input correctly
  - Test bank account encryption in database
  - Verify duplicate refund prevention works
  - Verify refund status displays in booking detail after submission
  - Ask user if questions arise before proceeding to UC22

---

### UC22: Download Invoice PDF (feature/mvh-download-invoice)

- [ ] 37. Setup UC22 branch and extend backend
  - Create branch from main: `git checkout -b feature/mvh-download-invoice`
  - Add iText PDF dependencies to pom.xml if not present
  - Create InvoiceGenerator service class
  - Create placeholder GET /api/bookings/{id}/invoice endpoint
  - _Requirements: 9.1, 10.1_

- [ ] 38. Implement backend invoice generation service
  - [ ] 38.1 Create InvoiceDataDTO
    - Include invoice metadata: invoiceNumber, issueDate
    - Include complete booking details with nested DTOs
    - Include line items list with description, quantity, unit price, amount
    - Include calculated totals: subtotal, tax, total
    - _Requirements: 5.4, 5.5, 5.6, 14.1_

  - [ ] 38.2 Create InvoiceGenerator service
    - Implement parseBookingToInvoiceData(Booking) method
    - Parse booking and payment data into InvoiceDataDTO
    - Generate unique invoice number based on booking reference
    - Calculate line items: tour package, service fee, tax
    - Calculate subtotal, tax, and total amounts
    - _Requirements: 14.1, 14.2_

  - [ ] 38.3 Implement PDF pretty printer
    - Create generatePDF(InvoiceDataDTO) method using iText
    - Design professional PDF layout with company logo placeholder
    - Add invoice header: company info, invoice number, issue date
    - Add "Bill To" section with customer information
    - Add tour details section with highlighted background
    - Add itemized charges table with proper formatting
    - Add payment information section
    - Add footer with terms and thank you message
    - _Requirements: 5.3, 5.7, 5.9, 14.2, 14.4_

  - [ ] 38.4 Implement invoice validation and error handling
    - Validate booking status is CONFIRMED or COMPLETED
    - Throw exception if booking not eligible for invoice
    - Handle missing optional fields gracefully (don't show null/empty)
    - Ensure PDF generation produces valid, openable PDF files
    - _Requirements: 5.2, 14.3, 14.6_

  - [ ] 38.5 Ensure invoice idempotency
    - Generate invoice with deterministic data (no timestamps that change)
    - Ensure same booking produces identical PDF output every time
    - Use consistent formatting, fonts, spacing
    - _Requirements: 14.5_


- [ ] 39. Implement invoice download REST API endpoints
  - [ ] 39.1 Complete GET /api/bookings/{id}/invoice endpoint
    - Add @GetMapping("/{bookingId}/invoice")
    - Add Swagger @Operation annotation
    - Extract authenticated customer ID from SecurityContext
    - Validate booking ownership (authorization)
    - Call InvoiceGenerator to generate PDF
    - Set response headers: Content-Type: application/pdf, Content-Disposition: attachment
    - Return PDF byte array in response body
    - _Requirements: 5.1, 5.8, 5.11, 6.1_

  - [ ] 39.2 Create GET /api/bookings/{id}/invoice/preview endpoint
    - Return InvoiceDataDTO as JSON for frontend preview
    - Same authorization and validation as download endpoint
    - Use for displaying invoice preview modal
    - _Requirements: 5.1_

  - [ ] 39.3 Add exception handlers for invoice errors
    - Handle BusinessRuleException (invalid status) → 400 Bad Request
    - Handle PDF generation errors → 500 Internal Server Error
    - Log detailed error information server-side
    - Return user-friendly error messages
    - _Requirements: 5.2, 7.2, 7.3_

  - [ ] 39.4 Add Swagger documentation for invoice endpoints
    - Document both download and preview endpoints
    - Add example responses: 200 OK with PDF binary, 400 Bad Request, 403 Forbidden
    - Document Content-Type and Content-Disposition headers
    - Test endpoints in Swagger UI
    - _Requirements: 10.1, 10.2, 10.3, 10.5_

- [ ] 40. Create invoice preview modal with Tailwind CSS
  - [ ] 40.1 Implement invoice preview modal structure
    - Create modal with fixed inset-0 positioning
    - Add backdrop with blur effect
    - Create large modal panel (max-w-4xl) for invoice preview
    - Add gradient header with "Invoice Preview" title
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 40.2 Implement invoice preview modal header
    - Display "Invoice Preview" title with document icon
    - Add "Download PDF" button in header
    - Add close (X) button
    - Use primary gradient background
    - _Requirements: 5.1_

  - [ ] 40.3 Implement professional invoice layout in modal
    - Display complete invoice HTML matching PDF layout
    - Include company logo, invoice header with number and dates
    - Display "Bill To" section with customer info
    - Show tour details in highlighted card
    - Display itemized table with borders and proper alignment
    - Show totals section with subtotal, tax, and total
    - Include payment information with success badge
    - Add terms and footer section
    - _Requirements: 5.3, 5.4, 5.5, 5.6, 5.7, 5.9_

  - [ ] 40.4 Make invoice content scrollable
    - Wrap invoice in scrollable container with max-h-[calc(90vh-80px)]
    - Ensure all invoice content is visible and accessible
    - Add padding and shadow to invoice content
    - _Requirements: 5.8_


- [ ] 41. Implement frontend invoice functionality
  - [ ] 41.1 Create invoice API functions
    - Add getInvoicePreview(bookingId) to bookingApi.js
    - Add downloadInvoice(bookingId) to bookingApi.js
    - Include JWT token in Authorization header
    - Handle blob response for PDF download
    - _Requirements: 5.11, 6.1_

  - [ ] 41.2 Implement "Download Invoice" button in booking detail
    - Show button only if status is CONFIRMED or COMPLETED
    - Add click handler to open invoice preview modal
    - _Requirements: 5.1, 5.2_

  - [ ] 41.3 Implement invoice preview modal logic
    - Fetch invoice preview data when modal opens
    - Display loading state while fetching
    - Populate invoice template with fetched data
    - Format currency and dates properly
    - _Requirements: 5.8_

  - [ ] 41.4 Implement PDF download functionality
    - Add click handler to "Download PDF" button in modal
    - Call downloadInvoice API
    - Create blob URL from response
    - Trigger browser download with filename: invoice-{bookingReference}.pdf
    - Show success notification after download starts
    - _Requirements: 5.8_

  - [ ] 41.5 Handle invoice errors
    - Show error notification if invoice not available (wrong status)
    - Show error notification if PDF generation fails
    - Display appropriate error messages based on error type
    - _Requirements: 5.2, 7.2_

- [ ] 42. Testing for UC22
  - [ ]* 42.1 Write property test for Invoice PDF Generation Success (Property 17)
    - **Property 17: Invoice PDF Generation Success**
    - Generate bookings with CONFIRMED or COMPLETED status
    - Verify invoice generation succeeds and produces valid PDF
    - Verify PDF contains all required sections: company info, booking details, customer info, payment info, line items, totals
    - **Validates: Requirements 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 14.3**

  - [ ]* 42.2 Write property test for Invoice Generation Idempotency (Property 18)
    - **Property 18: Invoice Generation Idempotency**
    - Generate arbitrary bookings
    - Generate invoice multiple times for same booking
    - Verify PDFs are byte-for-byte identical
    - **Validates: Requirements 14.5**

  - [ ]* 42.3 Write property test for Invoice Graceful Degradation (Property 19)
    - **Property 19: Invoice Graceful Degradation**
    - Generate bookings with missing optional fields (billing address, notes)
    - Verify invoice generation succeeds without displaying null/empty values
    - **Validates: Requirements 14.6**

  - [ ]* 42.4 Write unit tests for invoice generator
    - Test parseBookingToInvoiceData extracts all required data
    - Test generatePDF produces valid PDF file
    - Test invoice generation throws exception for PENDING/CANCELLED bookings
    - Test idempotency: same booking produces identical output
    - Test missing optional fields are handled gracefully
    - Verify PDF can be opened in standard PDF readers

  - [ ]* 42.5 Write integration tests for invoice API
    - Test GET /api/bookings/{id}/invoice returns 200 with PDF for CONFIRMED booking
    - Test GET /api/bookings/{id}/invoice returns 200 with PDF for COMPLETED booking
    - Test GET /api/bookings/{id}/invoice returns 400 for PENDING booking
    - Test GET /api/bookings/{id}/invoice returns 400 for CANCELLED booking
    - Test GET /api/bookings/{id}/invoice returns 403 for other customer's booking
    - Test GET /api/bookings/{id}/invoice returns 401 without JWT
    - Test GET /api/bookings/{id}/invoice/preview returns JSON invoice data
    - Use Swagger UI to test all scenarios

  - [ ]* 42.6 Test invoice preview modal and download
    - Test modal opens when "Download Invoice" clicked
    - Test invoice preview displays all information correctly
    - Test "Download PDF" button triggers file download
    - Test invoice filename format: invoice-{bookingReference}.pdf
    - Test modal closes on backdrop click and X button
    - Test error handling when invoice not available


- [ ] 43. Git workflow and PR for UC22
  - Commit all changes to feature/mvh-download-invoice branch
  - Push branch: `git push -u origin feature/mvh-download-invoice`
  - Create Pull Request for code review (do NOT merge to main)
  - Add PR description with screenshots of invoice preview modal and sample PDF
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 44. Checkpoint - UC22 Complete
  - Ensure all tests pass
  - Verify invoice PDF generates correctly with all required information
  - Test PDF opens in multiple PDF readers (Adobe, browser, etc.)
  - Verify idempotency: same booking produces identical PDF
  - Test invoice preview modal displays correctly
  - Test download functionality works in all browsers
  - Ask user if questions arise before proceeding to authentication and cross-cutting concerns

---

### Authentication and Cross-Cutting Concerns

- [ ] 45. Implement authentication and authorization
  - [ ] 45.1 Verify JWT authentication configuration
    - Confirm JWT token validation in Spring Security configuration
    - Verify JWT filter extracts customer ID from token
    - Test authentication filter on all booking endpoints
    - _Requirements: 6.1, 6.2_

  - [ ] 45.2 Implement authorization checks across all services
    - Verify all service methods check customer ownership
    - Add authorization checks where missing
    - Ensure consistent ForbiddenException throwing
    - _Requirements: 6.3, 6.4_

  - [ ]* 45.3 Write property test for Authentication Required (Property 1)
    - **Property 1: Authentication Required**
    - Test all booking management endpoints without JWT token
    - Verify all requests return 401 Unauthorized
    - **Validates: Requirements 6.1, 6.2, 6.5**

  - [ ]* 45.4 Write unit tests for authorization
    - Test authorization check for each service method
    - Verify ForbiddenException thrown when customer IDs don't match
    - Test authorization passes when customer IDs match

- [ ] 46. Implement error handling and validation
  - [ ] 46.1 Create global exception handler
    - Add @ControllerAdvice class with @ExceptionHandler methods
    - Handle NotFoundException → 404 Not Found
    - Handle ForbiddenException → 403 Forbidden
    - Handle BusinessRuleException → 400 Bad Request
    - Handle MethodArgumentNotValidException → 400 Bad Request with field errors
    - Handle generic Exception → 500 Internal Server Error
    - _Requirements: 7.2_

  - [ ] 46.2 Implement consistent error response structure
    - Create ErrorResponse DTO with timestamp, status, error, message, path, traceId
    - Return ErrorResponse from all exception handlers
    - Include detailed validation messages for field errors
    - _Requirements: 7.2_

  - [ ] 46.3 Implement input validation across DTOs
    - Add validation annotations: @NotBlank, @NotNull, @Min, @Max, @Email
    - Test validation in controller layer with @Valid annotation
    - Return 400 with field-specific error messages
    - _Requirements: 7.1_

  - [ ]* 46.4 Write property test for Database Error Handling (Property 23)
    - **Property 23: Database Error Handling**
    - Simulate database errors (connection failure, constraint violation)
    - Verify system returns appropriate error response and logs detailed error
    - **Validates: Requirements 7.3**

  - [ ]* 46.5 Write property test for Optimistic Locking (Property 24)
    - **Property 24: Optimistic Locking for Concurrent Modifications**
    - Simulate concurrent booking modifications
    - Verify second modification detects version conflict and fails
    - **Validates: Requirements 7.4**

  - [ ]* 46.6 Write unit tests for error handling
    - Test all exception handlers return correct status codes
    - Test error response structure is consistent
    - Test validation errors include field names and messages
    - Test unexpected errors return 500 with generic message


- [ ] 47. Implement status management
  - [ ] 47.1 Verify booking status transitions
    - Ensure BookingStatusHistory entity exists and is properly linked
    - Implement status transition recording in service methods
    - Test status history ordering (most recent first)
    - _Requirements: 11.2, 11.3, 11.4_

  - [ ] 47.2 Implement status transition validation
    - Create method to validate status transitions
    - Prevent invalid transitions: Cancelled→Confirmed, Completed→Pending, etc.
    - Throw BusinessRuleException for invalid transitions
    - _Requirements: 11.5_

  - [ ]* 47.3 Write property test for Initial Booking Status (Property 20)
    - **Property 20: Initial Booking Status**
    - Generate newly created bookings
    - Verify initial status is set to PENDING
    - **Validates: Requirements 11.2**

  - [ ]* 47.4 Write property test for Status Transition Timestamps (Property 21)
    - **Property 21: Status Transition Timestamps**
    - Generate bookings with status transitions
    - Verify timestamp is recorded for each transition
    - **Validates: Requirements 11.3, 12.3**

  - [ ]* 47.5 Write property test for Invalid Status Transition Prevention (Property 22)
    - **Property 22: Invalid Status Transition Prevention**
    - Generate invalid status transition attempts
    - Verify all invalid transitions are rejected with appropriate error
    - **Validates: Requirements 11.5**

  - [ ]* 47.6 Write unit tests for status management
    - Test status transition recording creates history entry
    - Test invalid transitions throw exceptions
    - Test status history ordering
    - Test refund status transitions

- [ ] 48. Implement performance optimizations
  - [ ] 48.1 Verify database indexes
    - Confirm indexes exist on: customer_id, status, departure_date
    - Add composite index on (customer_id, status) if not exists
    - Test query performance with EXPLAIN PLAN
    - _Requirements: 13.2_

  - [ ] 48.2 Implement query optimization
    - Use JOIN FETCH to prevent N+1 queries in all repository methods
    - Test that single query loads booking with all relationships
    - _Requirements: 13.1_

  - [ ] 48.3 Implement search debouncing
    - Add 300ms debounce to search input in frontend
    - Prevent excessive API calls while user is typing
    - _Requirements: 13.3_

  - [ ] 48.4 Test API response times
    - Test booking history endpoint responds within 500ms for 1000 bookings
    - Test booking detail endpoint responds quickly
    - Optimize slow queries if needed
    - _Requirements: 13.1_

- [ ] 49. Implement responsive UI across all pages
  - [ ] 49.1 Test mobile responsiveness (<768px)
    - Test all pages on mobile: single column layout, stacked elements
    - Verify touch-friendly buttons: minimum 44x44px tap targets
    - Test modals are full-width on mobile
    - _Requirements: 8.1, 8.6_

  - [ ] 49.2 Test tablet responsiveness (768-1024px)
    - Test all pages on tablet: 2-column grids, side-by-side layouts
    - Verify filters and search display properly
    - Test modals and cards adapt to tablet width
    - _Requirements: 8.2_

  - [ ] 49.3 Test desktop responsiveness (>1024px)
    - Test all pages on desktop: multi-column grids, full layouts
    - Verify sticky sidebar in booking detail works
    - Test hover effects and animations
    - _Requirements: 8.3_

  - [ ] 49.4 Verify Tailwind CSS implementation
    - Confirm no inline styles or Bootstrap classes used
    - Use only Tailwind utility classes
    - Verify custom animations are defined in Tailwind config
    - _Requirements: 8.7_


- [ ] 50. Final integration and testing
  - [ ] 50.1 Run complete test suite
    - Run all unit tests: `mvn test`
    - Run all property-based tests with jqwik
    - Verify all 24 correctness properties pass
    - Fix any failing tests
    - _Requirements: All property requirements_

  - [ ] 50.2 Test complete user workflows end-to-end
    - Test workflow: Login → View History → View Detail → Cancel → Request Refund
    - Test workflow: Login → View History → View Detail → Download Invoice
    - Test workflow with filters and search
    - Test all error scenarios (wrong customer, invalid status, etc.)

  - [ ] 50.3 Perform Swagger UI comprehensive testing
    - Test all endpoints in Swagger UI with various inputs
    - Test success cases: 200 OK, 201 Created
    - Test error cases: 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 500 Internal Server Error
    - Verify all request/response examples work
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 50.4 Test across browsers
    - Test in Chrome, Firefox, Safari, Edge
    - Verify all functionality works in each browser
    - Test PDF download in each browser

  - [ ] 50.5 Accessibility testing
    - Test keyboard navigation through all pages
    - Verify screen reader compatibility (basic check)
    - Note: Full WCAG compliance requires expert review
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 50.6 Security testing
    - Test JWT authentication on all endpoints
    - Test authorization: verify customers cannot access other customers' data
    - Verify bank account numbers are encrypted in database
    - Test SQL injection prevention (parameterized queries)
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

---

### Documentation and Deployment Preparation

- [ ] 51. Finalize documentation
  - [ ] 51.1 Update API documentation
    - Verify all Swagger annotations are complete
    - Add missing endpoint descriptions
    - Verify all example requests and responses are accurate
    - Test Swagger UI is accessible and functional

  - [ ] 51.2 Add code comments for mock data
    - Comment all fake/mock data: "// MOCK DATA - Replace with real data"
    - Mark temporary test data clearly
    - _Requirements: 9.4, 9.5_

  - [ ] 51.3 Create README for MVH Booking Management
    - Document feature overview and use cases
    - List API endpoints with brief descriptions
    - Document frontend pages and navigation
    - Include setup instructions for development
    - Document testing approach (unit + property tests)

- [ ] 52. Prepare for deployment
  - [ ] 52.1 Review all Git branches
    - Verify each UC has its own branch
    - Verify no direct commits to main
    - Verify all PRs are created and ready for review
    - _Requirements: 9.1, 9.2_

  - [ ] 52.2 Code quality review
    - Run code formatter and linter
    - Fix any code style issues
    - Remove unused imports and variables
    - Remove debug console.log statements

  - [ ] 52.3 Performance review
    - Verify database indexes are in place
    - Check for N+1 query problems
    - Test API response times
    - Verify frontend loading states work correctly

  - [ ] 52.4 Security review
    - Verify all endpoints require authentication
    - Verify authorization checks on all operations
    - Verify sensitive data encryption (bank accounts)
    - Verify error messages don't leak sensitive information

- [ ] 53. Final checkpoint - Feature complete
  - All 5 use cases (UC18-UC22) implemented and tested
  - All 24 correctness properties validated with property-based tests
  - All unit tests and integration tests passing
  - All Swagger documentation complete and tested
  - All frontend pages responsive (mobile, tablet, desktop)
  - All Git branches created with PRs ready
  - No direct commits to main branch
  - Code quality reviewed and approved
  - Ready for deployment to staging/production

---

## Notes

### Task Marking Conventions

- **Tasks marked with `*`**: Optional test-related sub-tasks that can be skipped for faster MVP
- **All other tasks**: Required implementation tasks that must be completed
- **Checkpoints**: Pause points to verify work and ask questions before proceeding

### Testing Strategy

- **Property-based tests**: Use jqwik with minimum 100 iterations per property
- **Unit tests**: Verify specific examples, edge cases, error conditions
- **Integration tests**: Test API endpoints with MockMvc and real database
- **Swagger tests**: Manual testing in Swagger UI for all endpoints
- **UI tests**: Manual testing on actual devices (mobile, tablet, desktop)

### Git Workflow Reminder

1. Create feature branch for each UC: `git checkout -b feature/mvh-{uc-name}`
2. Commit changes incrementally with descriptive messages
3. Push branch: `git push -u origin feature/mvh-{uc-name}`
4. Create Pull Request (do NOT merge to main)
5. Add PR description with summary, testing notes, screenshots
6. Wait for code review approval before merging (not part of this workflow)

### Frontend Development Notes

- **Use Tailwind CSS only**: No inline styles, no Bootstrap, no custom CSS files
- **Responsive breakpoints**: sm (640px), md (768px), lg (1024px), xl (1280px)
- **Touch targets**: Minimum 44x44px for mobile buttons
- **Animations**: Define custom animations in tailwind.config.js
- **Icons**: Use SVG icons inline or from icon library (Heroicons, Feather Icons)

### Backend Development Notes

- **Swagger first**: Document endpoints before implementation
- **Security first**: Add authentication/authorization before business logic
- **Validation first**: Validate input before processing
- **Error handling**: Use consistent error response structure
- **Database**: Use parameterized queries, add indexes, prevent N+1 queries
- **Testing**: Write property tests alongside implementation

### Property-Based Testing Notes

- Each correctness property from design document must have corresponding jqwik test
- Use `@Property` annotation with `tries = 100` minimum
- Generate test data with `@ForAll` and custom `@Provide` methods
- Use `Assume.that()` for preconditions
- Tag each property test with property number and requirements it validates

### Success Criteria

✅ All 5 use cases fully implemented and tested
✅ All 24 correctness properties validated
✅ All APIs documented in Swagger and testable
✅ All UIs responsive across mobile, tablet, desktop
✅ All Git branches with PRs created (not merged to main)
✅ No direct commits to main branch
✅ Authentication and authorization working correctly
✅ Bank account encryption implemented
✅ Invoice PDF generation working correctly
✅ All error handling and validation in place
