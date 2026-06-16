# Design Document: MVH Booking Management System

## Overview

The MVH Booking Management system is a customer-facing web application built with Spring Boot backend and a modern, responsive frontend using Tailwind CSS. The system enables customers to manage their tour bookings through five primary interfaces: booking history view, booking detail view, cancellation flow, refund request flow, and invoice generation.

### Design Goals

1. **Modern UI/UX**: Professional, clean interface with smooth animations and transitions
2. **Responsive Design**: Mobile-first approach supporting all device sizes
3. **Performance**: Fast load times with optimized API calls and client-side caching
4. **Accessibility**: WCAG 2.1 AA compliant with keyboard navigation support
5. **Security**: Authentication-based access with proper authorization checks
6. **Maintainability**: Clear separation of concerns with reusable component library

### Technology Stack

- **Backend**: Spring Boot, Spring Security, JPA/Hibernate, iText for PDF generation
- **Frontend**: HTML5, Tailwind CSS 3.x, Vanilla JavaScript (or framework TBD)
- **Database**: SQL Server with indexed queries
- **API Documentation**: Swagger/OpenAPI 3.0
- **Authentication**: JWT-based authentication

## Architecture

### System Architecture


```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Booking    │  │   Booking    │  │  Refund &    │     │
│  │   History    │  │   Detail     │  │  Cancel      │     │
│  │   Page       │  │   Page       │  │  Components  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ REST API (JSON)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway Layer                        │
│              (Authentication & Authorization)                │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Booking    │  │ Cancellation │  │   Refund     │     │
│  │   Service    │  │   Service    │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │   Invoice    │  │    Search    │                        │
│  │   Generator  │  │   Service    │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Data Access Layer                         │
│              (JPA Repositories with Indexes)                │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Database (SQL Server)                     │
│     Bookings │ Customers │ Refunds │ StatusHistory         │
└─────────────────────────────────────────────────────────────┘
```

### Frontend Architecture

The frontend follows a component-based architecture with:

1. **Page Components**: Top-level pages (History, Detail)
2. **Feature Components**: Complex widgets (StatusTimeline, BookingCard, CancelModal)
3. **UI Components**: Reusable elements (Button, Badge, Card, Modal, Form)
4. **State Management**: Client-side state for filters, search, and pagination
5. **API Client**: Centralized fetch wrapper with error handling and auth token injection

## Components and Interfaces



### Design System

#### Color Palette

```css
/* Primary Colors */
--color-primary-50: #eff6ff;    /* Lightest blue */
--color-primary-100: #dbeafe;
--color-primary-500: #3b82f6;   /* Primary blue */
--color-primary-600: #2563eb;   /* Hover state */
--color-primary-700: #1d4ed8;   /* Active state */

/* Status Colors */
--color-success-50: #f0fdf4;
--color-success-500: #22c55e;   /* Confirmed/Completed */
--color-warning-50: #fffbeb;
--color-warning-500: #f59e0b;   /* Pending */
--color-danger-50: #fef2f2;
--color-danger-500: #ef4444;    /* Cancelled/Error */

/* Neutral Colors */
--color-gray-50: #f9fafb;
--color-gray-100: #f3f4f6;
--color-gray-200: #e5e7eb;
--color-gray-500: #6b7280;
--color-gray-700: #374151;
--color-gray-900: #111827;
```

#### Typography Scale

```
text-xs:   0.75rem (12px)  - Small labels, captions
text-sm:   0.875rem (14px) - Body text, table content
text-base: 1rem (16px)     - Default body text
text-lg:   1.125rem (18px) - Subheadings
text-xl:   1.25rem (20px)  - Card titles
text-2xl:  1.5rem (24px)   - Page headings
text-3xl:  1.875rem (30px) - Hero headings
```

#### Spacing Scale (4px base unit)

```
p-1: 0.25rem (4px)
p-2: 0.5rem (8px)
p-3: 0.75rem (12px)
p-4: 1rem (16px)
p-6: 1.5rem (24px)
p-8: 2rem (32px)
p-12: 3rem (48px)
```

#### Shadow System

```
shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05)
shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1)
shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1)
shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1)
```

### Component Library



#### Button Component

```html
<!-- Primary Button -->
<button class="inline-flex items-center justify-center px-6 py-3 
               bg-primary-600 hover:bg-primary-700 
               text-white font-medium rounded-lg 
               shadow-sm hover:shadow-md 
               transition-all duration-200 
               focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
               disabled:opacity-50 disabled:cursor-not-allowed">
  Button Text
</button>

<!-- Secondary Button -->
<button class="inline-flex items-center justify-center px-6 py-3 
               bg-white hover:bg-gray-50 
               text-gray-700 font-medium rounded-lg 
               border border-gray-300 
               shadow-sm hover:shadow-md 
               transition-all duration-200 
               focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2">
  Button Text
</button>

<!-- Danger Button -->
<button class="inline-flex items-center justify-center px-6 py-3 
               bg-danger-500 hover:bg-danger-600 
               text-white font-medium rounded-lg 
               shadow-sm hover:shadow-md 
               transition-all duration-200 
               focus:outline-none focus:ring-2 focus:ring-danger-500 focus:ring-offset-2">
  Cancel Booking
</button>
```

#### Badge Component

```html
<!-- Success Badge (Confirmed) -->
<span class="inline-flex items-center px-3 py-1 
             rounded-full text-xs font-medium 
             bg-success-50 text-success-700 
             border border-success-200">
  <svg class="w-3 h-3 mr-1" fill="currentColor">...</svg>
  Confirmed
</span>

<!-- Warning Badge (Pending) -->
<span class="inline-flex items-center px-3 py-1 
             rounded-full text-xs font-medium 
             bg-warning-50 text-warning-700 
             border border-warning-200">
  <svg class="w-3 h-3 mr-1 animate-pulse" fill="currentColor">...</svg>
  Pending
</span>

<!-- Danger Badge (Cancelled) -->
<span class="inline-flex items-center px-3 py-1 
             rounded-full text-xs font-medium 
             bg-danger-50 text-danger-700 
             border border-danger-200">
  <svg class="w-3 h-3 mr-1" fill="currentColor">...</svg>
  Cancelled
</span>

<!-- Info Badge (Completed) -->
<span class="inline-flex items-center px-3 py-1 
             rounded-full text-xs font-medium 
             bg-gray-100 text-gray-700 
             border border-gray-200">
  <svg class="w-3 h-3 mr-1" fill="currentColor">...</svg>
  Completed
</span>
```

#### Card Component

```html
<div class="bg-white rounded-xl shadow-md hover:shadow-lg 
            transition-shadow duration-200 
            border border-gray-100 
            overflow-hidden">
  <div class="p-6">
    <!-- Card content -->
  </div>
</div>
```



### 1. Booking History Page (UC18)

#### Page Layout

```
┌──────────────────────────────────────────────────────────────┐
│  Header: "My Bookings" + User Avatar                        │
├──────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Statistics Cards (Row of 4)                          │ │
│  │  [Total] [Confirmed] [Pending] [Total Spent]          │ │
│  └────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌──────────────────────────────────┐ │
│  │  Filters        │  │  Search Bar                       │ │
│  │  - Status       │  │  🔍 Search by tour name...        │ │
│  │  - Date Range   │  └──────────────────────────────────┘ │
│  │  - Price Range  │                                        │
│  └─────────────────┘                                        │
├──────────────────────────────────────────────────────────────┤
│  Booking Cards (Grid/List)                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Card 1: Tour Image | Details | Status | Actions      │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Card 2: Tour Image | Details | Status | Actions      │ │
│  └────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────┤
│  Pagination: ← 1 2 3 ... 10 →                              │
└──────────────────────────────────────────────────────────────┘
```

#### Statistics Cards Component

```html
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
  <!-- Total Bookings Card -->
  <div class="bg-gradient-to-br from-blue-500 to-blue-600 
              rounded-xl shadow-lg p-6 text-white 
              transform hover:scale-105 transition-transform duration-200">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-blue-100 text-sm font-medium">Total Bookings</p>
        <p class="text-3xl font-bold mt-2">24</p>
      </div>
      <div class="bg-white bg-opacity-20 rounded-full p-3">
        <svg class="w-8 h-8" fill="currentColor">
          <!-- Calendar icon -->
        </svg>
      </div>
    </div>
  </div>

  <!-- Confirmed Bookings Card -->
  <div class="bg-gradient-to-br from-green-500 to-green-600 
              rounded-xl shadow-lg p-6 text-white 
              transform hover:scale-105 transition-transform duration-200">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-green-100 text-sm font-medium">Confirmed</p>
        <p class="text-3xl font-bold mt-2">18</p>
      </div>
      <div class="bg-white bg-opacity-20 rounded-full p-3">
        <svg class="w-8 h-8" fill="currentColor">
          <!-- Checkmark icon -->
        </svg>
      </div>
    </div>
  </div>

  <!-- Pending Bookings Card -->
  <div class="bg-gradient-to-br from-yellow-500 to-yellow-600 
              rounded-xl shadow-lg p-6 text-white 
              transform hover:scale-105 transition-transform duration-200">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-yellow-100 text-sm font-medium">Pending</p>
        <p class="text-3xl font-bold mt-2">3</p>
      </div>
      <div class="bg-white bg-opacity-20 rounded-full p-3">
        <svg class="w-8 h-8 animate-pulse" fill="currentColor">
          <!-- Clock icon -->
        </svg>
      </div>
    </div>
  </div>

  <!-- Total Spent Card -->
  <div class="bg-gradient-to-br from-purple-500 to-purple-600 
              rounded-xl shadow-lg p-6 text-white 
              transform hover:scale-105 transition-transform duration-200">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-purple-100 text-sm font-medium">Total Spent</p>
        <p class="text-3xl font-bold mt-2">$12,450</p>
      </div>
      <div class="bg-white bg-opacity-20 rounded-full p-3">
        <svg class="w-8 h-8" fill="currentColor">
          <!-- Dollar icon -->
        </svg>
      </div>
    </div>
  </div>
</div>
```



#### Search and Filter Component

```html
<div class="mb-8 space-y-4">
  <!-- Search Bar -->
  <div class="relative">
    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
      <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor">
        <!-- Search icon -->
      </svg>
    </div>
    <input type="text" 
           placeholder="Search by tour name, booking reference, or destination..."
           class="block w-full pl-12 pr-4 py-3 
                  border border-gray-300 rounded-lg 
                  focus:ring-2 focus:ring-primary-500 focus:border-transparent 
                  transition-all duration-200
                  text-gray-900 placeholder-gray-400">
  </div>

  <!-- Filter Pills Row -->
  <div class="flex flex-wrap gap-3">
    <!-- Status Filter Dropdown -->
    <div class="relative inline-block">
      <button class="inline-flex items-center px-4 py-2 
                     bg-white border border-gray-300 rounded-lg 
                     hover:bg-gray-50 transition-colors duration-200">
        <svg class="w-4 h-4 mr-2 text-gray-500" fill="currentColor">
          <!-- Filter icon -->
        </svg>
        <span class="text-sm font-medium text-gray-700">Status</span>
        <svg class="w-4 h-4 ml-2 text-gray-500" fill="currentColor">
          <!-- Chevron down -->
        </svg>
      </button>
      <!-- Dropdown menu (hidden by default) -->
      <div class="absolute z-10 mt-2 w-56 rounded-lg shadow-lg 
                  bg-white border border-gray-200 hidden">
        <div class="p-2">
          <label class="flex items-center px-3 py-2 hover:bg-gray-50 
                        rounded-md cursor-pointer transition-colors">
            <input type="checkbox" class="rounded text-primary-600 
                                          focus:ring-primary-500">
            <span class="ml-3 text-sm text-gray-700">All Status</span>
          </label>
          <label class="flex items-center px-3 py-2 hover:bg-gray-50 
                        rounded-md cursor-pointer transition-colors">
            <input type="checkbox" class="rounded text-primary-600">
            <span class="ml-3 text-sm text-gray-700">Confirmed</span>
          </label>
          <label class="flex items-center px-3 py-2 hover:bg-gray-50 
                        rounded-md cursor-pointer transition-colors">
            <input type="checkbox" class="rounded text-primary-600">
            <span class="ml-3 text-sm text-gray-700">Pending</span>
          </label>
          <label class="flex items-center px-3 py-2 hover:bg-gray-50 
                        rounded-md cursor-pointer transition-colors">
            <input type="checkbox" class="rounded text-primary-600">
            <span class="ml-3 text-sm text-gray-700">Completed</span>
          </label>
          <label class="flex items-center px-3 py-2 hover:bg-gray-50 
                        rounded-md cursor-pointer transition-colors">
            <input type="checkbox" class="rounded text-primary-600">
            <span class="ml-3 text-sm text-gray-700">Cancelled</span>
          </label>
        </div>
      </div>
    </div>

    <!-- Date Range Filter -->
    <button class="inline-flex items-center px-4 py-2 
                   bg-white border border-gray-300 rounded-lg 
                   hover:bg-gray-50 transition-colors duration-200">
      <svg class="w-4 h-4 mr-2 text-gray-500" fill="currentColor">
        <!-- Calendar icon -->
      </svg>
      <span class="text-sm font-medium text-gray-700">Date Range</span>
    </button>

    <!-- Price Range Filter -->
    <button class="inline-flex items-center px-4 py-2 
                   bg-white border border-gray-300 rounded-lg 
                   hover:bg-gray-50 transition-colors duration-200">
      <svg class="w-4 h-4 mr-2 text-gray-500" fill="currentColor">
        <!-- Dollar icon -->
      </svg>
      <span class="text-sm font-medium text-gray-700">Price Range</span>
    </button>

    <!-- Clear Filters Button (shown when filters active) -->
    <button class="inline-flex items-center px-4 py-2 
                   text-sm font-medium text-danger-600 
                   hover:text-danger-700 transition-colors duration-200">
      <svg class="w-4 h-4 mr-1" fill="currentColor">
        <!-- X icon -->
      </svg>
      Clear all
    </button>
  </div>
</div>
```



#### Booking Card Component

```html
<div class="bg-white rounded-xl shadow-md hover:shadow-xl 
            transition-all duration-300 
            border border-gray-100 overflow-hidden 
            group cursor-pointer">
  <div class="md:flex">
    <!-- Tour Image -->
    <div class="md:w-1/3 relative overflow-hidden">
      <img src="/tour-image.jpg" 
           alt="Tour name"
           class="w-full h-48 md:h-full object-cover 
                  group-hover:scale-110 transition-transform duration-500">
      <!-- Status Badge Overlay -->
      <div class="absolute top-4 right-4">
        <span class="inline-flex items-center px-3 py-1 
                     rounded-full text-xs font-medium 
                     bg-success-500 text-white shadow-lg backdrop-blur-sm">
          Confirmed
        </span>
      </div>
    </div>

    <!-- Booking Details -->
    <div class="p-6 md:w-2/3 flex flex-col justify-between">
      <div>
        <div class="flex items-start justify-between mb-3">
          <div>
            <h3 class="text-xl font-bold text-gray-900 mb-1 
                       group-hover:text-primary-600 transition-colors">
              Ha Long Bay Premium Cruise
            </h3>
            <p class="text-sm text-gray-500 flex items-center">
              <svg class="w-4 h-4 mr-1" fill="currentColor">
                <!-- Location pin icon -->
              </svg>
              Quang Ninh, Vietnam
            </p>
          </div>
          <button class="text-gray-400 hover:text-gray-600 transition-colors">
            <svg class="w-5 h-5" fill="currentColor">
              <!-- Heart icon for favorite -->
            </svg>
          </button>
        </div>

        <div class="grid grid-cols-2 gap-4 mb-4">
          <div class="flex items-center text-sm text-gray-600">
            <svg class="w-5 h-5 mr-2 text-gray-400" fill="none" stroke="currentColor">
              <!-- Calendar icon -->
            </svg>
            <div>
              <p class="text-xs text-gray-500">Departure</p>
              <p class="font-medium">Dec 25, 2024</p>
            </div>
          </div>
          <div class="flex items-center text-sm text-gray-600">
            <svg class="w-5 h-5 mr-2 text-gray-400" fill="none" stroke="currentColor">
              <!-- Clock icon -->
            </svg>
            <div>
              <p class="text-xs text-gray-500">Duration</p>
              <p class="font-medium">3 Days 2 Nights</p>
            </div>
          </div>
          <div class="flex items-center text-sm text-gray-600">
            <svg class="w-5 h-5 mr-2 text-gray-400" fill="none" stroke="currentColor">
              <!-- Users icon -->
            </svg>
            <div>
              <p class="text-xs text-gray-500">Guests</p>
              <p class="font-medium">2 Adults</p>
            </div>
          </div>
          <div class="flex items-center text-sm text-gray-600">
            <svg class="w-5 h-5 mr-2 text-gray-400" fill="none" stroke="currentColor">
              <!-- Receipt icon -->
            </svg>
            <div>
              <p class="text-xs text-gray-500">Booking Ref</p>
              <p class="font-medium">BK-2024-1234</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Price and Actions -->
      <div class="flex items-center justify-between pt-4 border-t border-gray-100">
        <div>
          <p class="text-xs text-gray-500">Total Price</p>
          <p class="text-2xl font-bold text-gray-900">$850.00</p>
        </div>
        <button class="inline-flex items-center px-5 py-2.5 
                       bg-primary-600 hover:bg-primary-700 
                       text-white font-medium rounded-lg 
                       shadow-sm hover:shadow-md 
                       transition-all duration-200
                       transform hover:scale-105">
          View Details
          <svg class="w-4 h-4 ml-2" fill="none" stroke="currentColor">
            <!-- Arrow right icon -->
          </svg>
        </button>
      </div>
    </div>
  </div>
</div>
```

#### Empty State Component

```html
<div class="flex flex-col items-center justify-center py-16 px-4">
  <div class="bg-gray-100 rounded-full p-6 mb-6">
    <svg class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor">
      <!-- Empty folder or search icon -->
    </svg>
  </div>
  <h3 class="text-xl font-semibold text-gray-900 mb-2">
    No bookings found
  </h3>
  <p class="text-gray-500 text-center max-w-md mb-6">
    We couldn't find any bookings matching your search criteria. 
    Try adjusting your filters or search terms.
  </p>
  <button class="inline-flex items-center px-6 py-3 
                 bg-primary-600 hover:bg-primary-700 
                 text-white font-medium rounded-lg 
                 transition-colors duration-200">
    Clear Filters
  </button>
</div>
```

#### Loading Skeleton

```html
<div class="bg-white rounded-xl shadow-md border border-gray-100 overflow-hidden">
  <div class="md:flex animate-pulse">
    <div class="md:w-1/3 bg-gray-300 h-48 md:h-full"></div>
    <div class="p-6 md:w-2/3 space-y-4">
      <div class="h-6 bg-gray-300 rounded w-3/4"></div>
      <div class="h-4 bg-gray-200 rounded w-1/2"></div>
      <div class="grid grid-cols-2 gap-4">
        <div class="h-10 bg-gray-200 rounded"></div>
        <div class="h-10 bg-gray-200 rounded"></div>
        <div class="h-10 bg-gray-200 rounded"></div>
        <div class="h-10 bg-gray-200 rounded"></div>
      </div>
      <div class="flex justify-between items-center pt-4">
        <div class="h-8 bg-gray-300 rounded w-24"></div>
        <div class="h-10 bg-gray-300 rounded w-32"></div>
      </div>
    </div>
  </div>
</div>
```



#### Pagination Component

```html
<div class="flex items-center justify-between border-t border-gray-200 
            bg-white px-4 py-4 sm:px-6 rounded-b-xl">
  <!-- Mobile Pagination -->
  <div class="flex flex-1 justify-between sm:hidden">
    <button class="relative inline-flex items-center px-4 py-2 
                   border border-gray-300 text-sm font-medium rounded-md 
                   text-gray-700 bg-white hover:bg-gray-50 
                   disabled:opacity-50 disabled:cursor-not-allowed">
      Previous
    </button>
    <button class="relative ml-3 inline-flex items-center px-4 py-2 
                   border border-gray-300 text-sm font-medium rounded-md 
                   text-gray-700 bg-white hover:bg-gray-50">
      Next
    </button>
  </div>

  <!-- Desktop Pagination -->
  <div class="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
    <div>
      <p class="text-sm text-gray-700">
        Showing <span class="font-medium">1</span> to 
        <span class="font-medium">10</span> of 
        <span class="font-medium">97</span> results
      </p>
    </div>
    <div>
      <nav class="isolate inline-flex -space-x-px rounded-md shadow-sm">
        <!-- Previous Button -->
        <button class="relative inline-flex items-center rounded-l-md 
                       px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 
                       hover:bg-gray-50 focus:z-20">
          <svg class="h-5 w-5" fill="currentColor">
            <!-- Chevron left -->
          </svg>
        </button>
        
        <!-- Page Numbers -->
        <button class="relative inline-flex items-center px-4 py-2 
                       text-sm font-semibold text-gray-900 ring-1 ring-inset 
                       ring-gray-300 hover:bg-gray-50 focus:z-20">
          1
        </button>
        <button class="relative inline-flex items-center px-4 py-2 
                       text-sm font-semibold bg-primary-600 text-white 
                       ring-1 ring-inset ring-primary-600 focus:z-20">
          2
        </button>
        <button class="relative inline-flex items-center px-4 py-2 
                       text-sm font-semibold text-gray-900 ring-1 ring-inset 
                       ring-gray-300 hover:bg-gray-50 focus:z-20">
          3
        </button>
        <span class="relative inline-flex items-center px-4 py-2 
                     text-sm font-semibold text-gray-700 ring-1 ring-inset 
                     ring-gray-300">
          ...
        </span>
        <button class="relative inline-flex items-center px-4 py-2 
                       text-sm font-semibold text-gray-900 ring-1 ring-inset 
                       ring-gray-300 hover:bg-gray-50 focus:z-20">
          10
        </button>
        
        <!-- Next Button -->
        <button class="relative inline-flex items-center rounded-r-md 
                       px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 
                       hover:bg-gray-50 focus:z-20">
          <svg class="h-5 w-5" fill="currentColor">
            <!-- Chevron right -->
          </svg>
        </button>
      </nav>
    </div>
  </div>
</div>
```

#### Responsive Behavior (UC18)

**Mobile (<768px)**:
- Single column layout
- Stacked statistics cards
- Collapsible filter sidebar/drawer
- Compact booking cards with image on top
- Touch-friendly 44x44px minimum tap targets

**Tablet (768px-1024px)**:
- Two-column grid for statistics cards
- Side-by-side filter and search
- Two-column booking card grid
- Maintained touch targets

**Desktop (>1024px)**:
- Four-column statistics cards
- Full-width search with inline filters
- Three-column booking card grid (optional)
- Enhanced hover effects



### 2. Booking Detail Page (UC19)

#### Page Layout

```
┌──────────────────────────────────────────────────────────────┐
│  Breadcrumb: My Bookings > Booking Details                  │
├──────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Hero Section: Tour Image + Title + Status            │ │
│  └────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────┤
│  2-Column Layout (Desktop) / Stacked (Mobile)                │
│  ┌─────────────────────┐  ┌──────────────────────────────┐ │
│  │  Left Column        │  │  Right Column (Sidebar)      │ │
│  │  ┌───────────────┐  │  │  ┌────────────────────────┐ │ │
│  │  │ Tour Info     │  │  │  │ Booking Summary Card  │ │ │
│  │  │ Card          │  │  │  │ - Price               │ │ │
│  │  └───────────────┘  │  │  │ - Payment Info        │ │ │
│  │  ┌───────────────┐  │  │  │ - Actions             │ │ │
│  │  │ Customer Info │  │  │  └────────────────────────┘ │ │
│  │  │ Card          │  │  │  ┌────────────────────────┐ │ │
│  │  └───────────────┘  │  │  │ Need Help? Support   │ │ │
│  │  ┌───────────────┐  │  │  │ Card                  │ │ │
│  │  │ Status        │  │  │  └────────────────────────┘ │ │
│  │  │ Timeline      │  │  │                              │ │
│  │  └───────────────┘  │  │                              │ │
│  │  ┌───────────────┐  │  │                              │ │
│  │  │ Payment       │  │  │                              │ │
│  │  │ Details Card  │  │  │                              │ │
│  │  └───────────────┘  │  │                              │ │
│  └─────────────────────┘  └──────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

#### Hero Section

```html
<div class="relative h-80 overflow-hidden rounded-2xl mb-8">
  <!-- Background Image with Overlay -->
  <img src="/tour-hero.jpg" 
       alt="Tour"
       class="absolute inset-0 w-full h-full object-cover">
  <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/40 to-transparent"></div>
  
  <!-- Content Overlay -->
  <div class="relative h-full flex flex-col justify-end p-8 text-white">
    <div class="mb-4">
      <span class="inline-flex items-center px-4 py-2 
                   rounded-full text-sm font-medium 
                   bg-success-500 text-white shadow-lg 
                   backdrop-blur-sm border border-white/20">
        <svg class="w-4 h-4 mr-2" fill="currentColor">
          <!-- Checkmark icon -->
        </svg>
        Confirmed
      </span>
    </div>
    <h1 class="text-4xl font-bold mb-2 drop-shadow-lg">
      Ha Long Bay Premium Cruise Experience
    </h1>
    <div class="flex items-center space-x-6 text-white/90">
      <div class="flex items-center">
        <svg class="w-5 h-5 mr-2" fill="currentColor">
          <!-- Location icon -->
        </svg>
        <span>Quang Ninh, Vietnam</span>
      </div>
      <div class="flex items-center">
        <svg class="w-5 h-5 mr-2" fill="currentColor">
          <!-- Calendar icon -->
        </svg>
        <span>Dec 25-27, 2024</span>
      </div>
      <div class="flex items-center">
        <svg class="w-5 h-5 mr-2" fill="currentColor">
          <!-- Receipt icon -->
        </svg>
        <span>BK-2024-1234</span>
      </div>
    </div>
  </div>
</div>
```



#### Tour Information Card

```html
<div class="bg-white rounded-xl shadow-md border border-gray-100 p-6 mb-6">
  <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center">
    <svg class="w-6 h-6 mr-3 text-primary-600" fill="currentColor">
      <!-- Info icon -->
    </svg>
    Tour Information
  </h2>

  <div class="space-y-6">
    <!-- Description -->
    <div>
      <h3 class="text-sm font-medium text-gray-500 mb-2">Description</h3>
      <p class="text-gray-700 leading-relaxed">
        Experience the breathtaking beauty of Ha Long Bay with our premium 
        3-day cruise. Explore limestone karsts, hidden caves, and pristine 
        beaches while enjoying luxury accommodations and authentic Vietnamese cuisine.
      </p>
    </div>

    <!-- Tour Details Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4 border-t border-gray-100">
      <div class="flex items-start">
        <div class="flex-shrink-0 bg-primary-50 rounded-lg p-3">
          <svg class="w-6 h-6 text-primary-600" fill="none" stroke="currentColor">
            <!-- Calendar icon -->
          </svg>
        </div>
        <div class="ml-4">
          <p class="text-sm font-medium text-gray-500">Departure Date</p>
          <p class="text-lg font-semibold text-gray-900">December 25, 2024</p>
          <p class="text-sm text-gray-500">9:00 AM</p>
        </div>
      </div>

      <div class="flex items-start">
        <div class="flex-shrink-0 bg-primary-50 rounded-lg p-3">
          <svg class="w-6 h-6 text-primary-600" fill="none" stroke="currentColor">
            <!-- Clock icon -->
          </svg>
        </div>
        <div class="ml-4">
          <p class="text-sm font-medium text-gray-500">Duration</p>
          <p class="text-lg font-semibold text-gray-900">3 Days 2 Nights</p>
          <p class="text-sm text-gray-500">December 25-27</p>
        </div>
      </div>

      <div class="flex items-start">
        <div class="flex-shrink-0 bg-primary-50 rounded-lg p-3">
          <svg class="w-6 h-6 text-primary-600" fill="none" stroke="currentColor">
            <!-- Location icon -->
          </svg>
        </div>
        <div class="ml-4">
          <p class="text-sm font-medium text-gray-500">Destination</p>
          <p class="text-lg font-semibold text-gray-900">Ha Long Bay</p>
          <p class="text-sm text-gray-500">Quang Ninh Province</p>
        </div>
      </div>

      <div class="flex items-start">
        <div class="flex-shrink-0 bg-primary-50 rounded-lg p-3">
          <svg class="w-6 h-6 text-primary-600" fill="none" stroke="currentColor">
            <!-- Users icon -->
          </svg>
        </div>
        <div class="ml-4">
          <p class="text-sm font-medium text-gray-500">Participants</p>
          <p class="text-lg font-semibold text-gray-900">2 Adults</p>
          <p class="text-sm text-gray-500">Standard Package</p>
        </div>
      </div>
    </div>

    <!-- Included Services -->
    <div class="pt-4 border-t border-gray-100">
      <h3 class="text-sm font-medium text-gray-500 mb-3">Included Services</h3>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
        <div class="flex items-center text-sm text-gray-700">
          <svg class="w-5 h-5 mr-2 text-success-500" fill="currentColor">
            <!-- Checkmark icon -->
          </svg>
          Luxury Cabin Accommodation
        </div>
        <div class="flex items-center text-sm text-gray-700">
          <svg class="w-5 h-5 mr-2 text-success-500" fill="currentColor">
            <!-- Checkmark icon -->
          </svg>
          All Meals Included
        </div>
        <div class="flex items-center text-sm text-gray-700">
          <svg class="w-5 h-5 mr-2 text-success-500" fill="currentColor">
            <!-- Checkmark icon -->
          </svg>
          Guided Cave Tours
        </div>
        <div class="flex items-center text-sm text-gray-700">
          <svg class="w-5 h-5 mr-2 text-success-500" fill="currentColor">
            <!-- Checkmark icon -->
          </svg>
          Kayaking & Swimming
        </div>
        <div class="flex items-center text-sm text-gray-700">
          <svg class="w-5 h-5 mr-2 text-success-500" fill="currentColor">
            <!-- Checkmark icon -->
          </svg>
          Round-trip Transportation
        </div>
        <div class="flex items-center text-sm text-gray-700">
          <svg class="w-5 h-5 mr-2 text-success-500" fill="currentColor">
            <!-- Checkmark icon -->
          </svg>
          English-speaking Guide
        </div>
      </div>
    </div>
  </div>
</div>
```



#### Status Timeline Component

```html
<div class="bg-white rounded-xl shadow-md border border-gray-100 p-6 mb-6">
  <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center">
    <svg class="w-6 h-6 mr-3 text-primary-600" fill="currentColor">
      <!-- Timeline icon -->
    </svg>
    Booking Status History
  </h2>

  <div class="flow-root">
    <ul class="relative border-l-2 border-gray-200 ml-3">
      <!-- Current Status -->
      <li class="mb-8 ml-6 relative">
        <span class="absolute -left-[1.6rem] flex items-center justify-center 
                     w-10 h-10 bg-success-500 rounded-full ring-4 ring-white 
                     shadow-lg animate-pulse">
          <svg class="w-5 h-5 text-white" fill="currentColor">
            <!-- Checkmark icon -->
          </svg>
        </span>
        <div class="bg-success-50 border border-success-200 rounded-lg p-4">
          <div class="flex items-center justify-between mb-1">
            <h3 class="text-lg font-semibold text-success-900">Confirmed</h3>
            <span class="text-sm font-medium text-success-700">Current</span>
          </div>
          <p class="text-sm text-success-700 mb-2">
            Your booking has been confirmed and you're all set for your tour!
          </p>
          <time class="text-xs text-success-600">
            December 15, 2024 at 2:30 PM
          </time>
        </div>
      </li>

      <!-- Past Status -->
      <li class="mb-8 ml-6 relative">
        <span class="absolute -left-[1.6rem] flex items-center justify-center 
                     w-10 h-10 bg-gray-200 rounded-full ring-4 ring-white shadow">
          <svg class="w-5 h-5 text-gray-600" fill="currentColor">
            <!-- Clock icon -->
          </svg>
        </span>
        <div class="bg-white border border-gray-200 rounded-lg p-4">
          <h3 class="text-lg font-semibold text-gray-900 mb-1">Pending</h3>
          <p class="text-sm text-gray-600 mb-2">
            Booking was awaiting payment confirmation
          </p>
          <time class="text-xs text-gray-500">
            December 14, 2024 at 10:15 AM
          </time>
        </div>
      </li>

      <!-- Initial Status -->
      <li class="ml-6 relative">
        <span class="absolute -left-[1.6rem] flex items-center justify-center 
                     w-10 h-10 bg-gray-200 rounded-full ring-4 ring-white shadow">
          <svg class="w-5 h-5 text-gray-600" fill="currentColor">
            <!-- Plus icon -->
          </svg>
        </span>
        <div class="bg-white border border-gray-200 rounded-lg p-4">
          <h3 class="text-lg font-semibold text-gray-900 mb-1">Created</h3>
          <p class="text-sm text-gray-600 mb-2">
            Booking was created and submitted
          </p>
          <time class="text-xs text-gray-500">
            December 14, 2024 at 10:00 AM
          </time>
        </div>
      </li>
    </ul>
  </div>
</div>
```

#### Customer Information Card

```html
<div class="bg-white rounded-xl shadow-md border border-gray-100 p-6 mb-6">
  <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center">
    <svg class="w-6 h-6 mr-3 text-primary-600" fill="currentColor">
      <!-- User icon -->
    </svg>
    Customer Information
  </h2>

  <div class="space-y-4">
    <div class="flex items-center pb-4 border-b border-gray-100">
      <div class="flex-shrink-0 w-16 h-16 bg-gradient-to-br 
                  from-primary-500 to-primary-600 
                  rounded-full flex items-center justify-center 
                  text-white text-xl font-bold shadow-md">
        JD
      </div>
      <div class="ml-4">
        <p class="text-lg font-semibold text-gray-900">John Doe</p>
        <p class="text-sm text-gray-500">Premium Member</p>
      </div>
    </div>

    <div class="space-y-3">
      <div class="flex items-center">
        <svg class="w-5 h-5 text-gray-400 mr-3" fill="none" stroke="currentColor">
          <!-- Email icon -->
        </svg>
        <div>
          <p class="text-xs text-gray-500">Email</p>
          <p class="text-sm font-medium text-gray-900">john.doe@example.com</p>
        </div>
      </div>

      <div class="flex items-center">
        <svg class="w-5 h-5 text-gray-400 mr-3" fill="none" stroke="currentColor">
          <!-- Phone icon -->
        </svg>
        <div>
          <p class="text-xs text-gray-500">Phone</p>
          <p class="text-sm font-medium text-gray-900">+84 123 456 789</p>
        </div>
      </div>

      <div class="flex items-center">
        <svg class="w-5 h-5 text-gray-400 mr-3" fill="none" stroke="currentColor">
          <!-- Users icon -->
        </svg>
        <div>
          <p class="text-xs text-gray-500">Participants</p>
          <p class="text-sm font-medium text-gray-900">2 Adults</p>
        </div>
      </div>
    </div>
  </div>
</div>
```



#### Payment Details Card

```html
<div class="bg-white rounded-xl shadow-md border border-gray-100 p-6 mb-6">
  <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center">
    <svg class="w-6 h-6 mr-3 text-primary-600" fill="currentColor">
      <!-- Credit card icon -->
    </svg>
    Payment Information
  </h2>

  <div class="space-y-4">
    <!-- Payment Status -->
    <div class="bg-success-50 border border-success-200 rounded-lg p-4 mb-4">
      <div class="flex items-center">
        <svg class="w-5 h-5 text-success-600 mr-2" fill="currentColor">
          <!-- Checkmark circle icon -->
        </svg>
        <span class="text-sm font-medium text-success-700">
          Payment Completed
        </span>
      </div>
    </div>

    <!-- Payment Details Grid -->
    <div class="space-y-3">
      <div class="flex justify-between py-3 border-b border-gray-100">
        <span class="text-sm text-gray-600">Transaction Reference</span>
        <span class="text-sm font-medium text-gray-900">TXN-2024-5678</span>
      </div>

      <div class="flex justify-between py-3 border-b border-gray-100">
        <span class="text-sm text-gray-600">Payment Method</span>
        <div class="flex items-center">
          <svg class="w-8 h-5 mr-2" fill="currentColor">
            <!-- Visa/Mastercard logo -->
          </svg>
          <span class="text-sm font-medium text-gray-900">•••• 4242</span>
        </div>
      </div>

      <div class="flex justify-between py-3 border-b border-gray-100">
        <span class="text-sm text-gray-600">Payment Date</span>
        <span class="text-sm font-medium text-gray-900">Dec 15, 2024</span>
      </div>

      <div class="flex justify-between py-3 border-b border-gray-100">
        <span class="text-sm text-gray-600">Subtotal</span>
        <span class="text-sm font-medium text-gray-900">$800.00</span>
      </div>

      <div class="flex justify-between py-3 border-b border-gray-100">
        <span class="text-sm text-gray-600">Service Fee</span>
        <span class="text-sm font-medium text-gray-900">$30.00</span>
      </div>

      <div class="flex justify-between py-3 border-b border-gray-100">
        <span class="text-sm text-gray-600">Tax</span>
        <span class="text-sm font-medium text-gray-900">$20.00</span>
      </div>

      <div class="flex justify-between py-4 bg-gray-50 -mx-6 px-6">
        <span class="text-lg font-semibold text-gray-900">Total Amount</span>
        <span class="text-2xl font-bold text-primary-600">$850.00</span>
      </div>
    </div>
  </div>
</div>
```

#### Booking Summary Sidebar (Desktop)

```html
<div class="sticky top-6 space-y-6">
  <!-- Price Summary Card -->
  <div class="bg-gradient-to-br from-primary-500 to-primary-600 
              rounded-xl shadow-lg p-6 text-white">
    <div class="mb-4">
      <p class="text-primary-100 text-sm font-medium mb-1">Total Price</p>
      <p class="text-4xl font-bold">$850.00</p>
      <p class="text-primary-100 text-sm mt-1">For 2 adults</p>
    </div>

    <div class="space-y-2 pt-4 border-t border-primary-400">
      <div class="flex justify-between text-sm">
        <span class="text-primary-100">Booking Status</span>
        <span class="font-medium">Confirmed</span>
      </div>
      <div class="flex justify-between text-sm">
        <span class="text-primary-100">Payment Status</span>
        <span class="font-medium">Paid</span>
      </div>
    </div>
  </div>

  <!-- Action Buttons Card -->
  <div class="bg-white rounded-xl shadow-md border border-gray-100 p-6">
    <h3 class="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
    <div class="space-y-3">
      <!-- Download Invoice Button -->
      <button class="w-full inline-flex items-center justify-center px-4 py-3 
                     bg-primary-600 hover:bg-primary-700 
                     text-white font-medium rounded-lg 
                     shadow-sm hover:shadow-md 
                     transition-all duration-200
                     transform hover:scale-105">
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor">
          <!-- Download icon -->
        </svg>
        Download Invoice
      </button>

      <!-- Cancel Booking Button -->
      <button class="w-full inline-flex items-center justify-center px-4 py-3 
                     bg-white hover:bg-gray-50 
                     text-danger-600 font-medium rounded-lg 
                     border border-danger-300 
                     transition-all duration-200">
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor">
          <!-- X icon -->
        </svg>
        Cancel Booking
      </button>
    </div>
  </div>

  <!-- Support Card -->
  <div class="bg-gray-50 rounded-xl border border-gray-200 p-6">
    <h3 class="text-lg font-semibold text-gray-900 mb-2">Need Help?</h3>
    <p class="text-sm text-gray-600 mb-4">
      Our support team is here to assist you with any questions or concerns.
    </p>
    <button class="w-full inline-flex items-center justify-center px-4 py-2.5 
                   bg-white hover:bg-gray-50 
                   text-gray-700 font-medium rounded-lg 
                   border border-gray-300 
                   transition-colors duration-200">
      <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor">
        <!-- Chat icon -->
      </svg>
      Contact Support
    </button>
  </div>
</div>
```



### 3. Cancel Booking Modal (UC20)

```html
<!-- Modal Backdrop -->
<div class="fixed inset-0 z-50 overflow-y-auto" aria-labelledby="modal-title" role="dialog">
  <!-- Backdrop Overlay with Blur -->
  <div class="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 
              text-center sm:block sm:p-0">
    <div class="fixed inset-0 bg-gray-900 bg-opacity-75 backdrop-blur-sm 
                transition-opacity" aria-hidden="true"></div>

    <!-- Modal Panel -->
    <div class="inline-block align-bottom bg-white rounded-2xl 
                text-left overflow-hidden shadow-2xl 
                transform transition-all 
                sm:my-8 sm:align-middle sm:max-w-lg sm:w-full
                animate-slide-up">
      
      <!-- Warning Header -->
      <div class="bg-gradient-to-r from-danger-500 to-danger-600 px-6 py-4">
        <div class="flex items-center">
          <div class="flex-shrink-0 bg-white bg-opacity-20 rounded-full p-2">
            <svg class="h-6 w-6 text-white" fill="none" stroke="currentColor">
              <!-- Alert triangle icon -->
            </svg>
          </div>
          <h3 class="ml-3 text-xl font-semibold text-white" id="modal-title">
            Cancel Booking
          </h3>
        </div>
      </div>

      <!-- Modal Content -->
      <div class="px-6 py-6">
        <!-- Warning Message -->
        <div class="mb-6">
          <p class="text-gray-700 leading-relaxed">
            Are you sure you want to cancel this booking? This action cannot be undone, 
            and you may be subject to cancellation fees according to our policy.
          </p>
        </div>

        <!-- Booking Summary -->
        <div class="bg-gray-50 rounded-lg p-4 mb-6">
          <h4 class="text-sm font-medium text-gray-900 mb-3">Booking Details</h4>
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-gray-600">Tour:</span>
              <span class="font-medium text-gray-900">Ha Long Bay Premium Cruise</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">Date:</span>
              <span class="font-medium text-gray-900">Dec 25-27, 2024</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">Amount:</span>
              <span class="font-medium text-gray-900">$850.00</span>
            </div>
          </div>
        </div>

        <!-- Cancellation Reason -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700 mb-3">
            Reason for Cancellation <span class="text-danger-500">*</span>
          </label>
          
          <div class="space-y-2">
            <label class="flex items-center p-3 border border-gray-300 rounded-lg 
                          hover:bg-gray-50 cursor-pointer transition-colors
                          has-[:checked]:border-primary-500 has-[:checked]:bg-primary-50">
              <input type="radio" name="reason" value="schedule_conflict" 
                     class="text-primary-600 focus:ring-primary-500">
              <span class="ml-3 text-sm text-gray-700">Schedule Conflict</span>
            </label>

            <label class="flex items-center p-3 border border-gray-300 rounded-lg 
                          hover:bg-gray-50 cursor-pointer transition-colors
                          has-[:checked]:border-primary-500 has-[:checked]:bg-primary-50">
              <input type="radio" name="reason" value="found_better_option" 
                     class="text-primary-600 focus:ring-primary-500">
              <span class="ml-3 text-sm text-gray-700">Found Better Option</span>
            </label>

            <label class="flex items-center p-3 border border-gray-300 rounded-lg 
                          hover:bg-gray-50 cursor-pointer transition-colors
                          has-[:checked]:border-primary-500 has-[:checked]:bg-primary-50">
              <input type="radio" name="reason" value="personal_reasons" 
                     class="text-primary-600 focus:ring-primary-500">
              <span class="ml-3 text-sm text-gray-700">Personal Reasons</span>
            </label>

            <label class="flex items-center p-3 border border-gray-300 rounded-lg 
                          hover:bg-gray-50 cursor-pointer transition-colors
                          has-[:checked]:border-primary-500 has-[:checked]:bg-primary-50">
              <input type="radio" name="reason" value="other" 
                     class="text-primary-600 focus:ring-primary-500">
              <span class="ml-3 text-sm text-gray-700">Other</span>
            </label>
          </div>
        </div>

        <!-- Optional Details -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Additional Details (Optional)
          </label>
          <textarea rows="3" 
                    placeholder="Please provide any additional information..."
                    class="block w-full px-4 py-3 
                           border border-gray-300 rounded-lg 
                           focus:ring-2 focus:ring-primary-500 focus:border-transparent 
                           transition-all duration-200
                           text-gray-900 placeholder-gray-400"></textarea>
        </div>

        <!-- Cancellation Policy Note -->
        <div class="bg-warning-50 border border-warning-200 rounded-lg p-4 mb-6">
          <div class="flex">
            <svg class="h-5 w-5 text-warning-600 mt-0.5" fill="currentColor">
              <!-- Info icon -->
            </svg>
            <div class="ml-3">
              <h4 class="text-sm font-medium text-warning-800 mb-1">
                Cancellation Policy
              </h4>
              <p class="text-xs text-warning-700">
                Cancellations made 7+ days before departure: Full refund<br>
                Cancellations made 3-7 days before: 50% refund<br>
                Cancellations made less than 3 days: No refund
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Modal Actions -->
      <div class="bg-gray-50 px-6 py-4 flex flex-col-reverse sm:flex-row 
                  sm:justify-end sm:space-x-3 space-y-3 space-y-reverse sm:space-y-0">
        <button type="button" 
                class="w-full sm:w-auto inline-flex justify-center items-center 
                       px-6 py-3 
                       bg-white hover:bg-gray-50 
                       text-gray-700 font-medium rounded-lg 
                       border border-gray-300 
                       shadow-sm 
                       transition-all duration-200">
          Keep Booking
        </button>
        <button type="button" 
                class="w-full sm:w-auto inline-flex justify-center items-center 
                       px-6 py-3 
                       bg-danger-600 hover:bg-danger-700 
                       text-white font-medium rounded-lg 
                       shadow-sm hover:shadow-md 
                       transition-all duration-200
                       transform hover:scale-105">
          Confirm Cancellation
        </button>
      </div>
    </div>
  </div>
</div>
```



### 4. Refund Request Page (UC21)

```html
<div class="max-w-3xl mx-auto">
  <!-- Page Header -->
  <div class="mb-8">
    <h1 class="text-3xl font-bold text-gray-900 mb-2">Request Refund</h1>
    <p class="text-gray-600">
      Please provide your bank information to process your refund for cancelled booking
    </p>
  </div>

  <!-- Booking Reference Card -->
  <div class="bg-primary-50 border border-primary-200 rounded-xl p-6 mb-8">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm text-primary-700 mb-1">Booking Reference</p>
        <p class="text-xl font-bold text-primary-900">BK-2024-1234</p>
        <p class="text-sm text-primary-600 mt-1">Ha Long Bay Premium Cruise</p>
      </div>
      <div class="text-right">
        <p class="text-sm text-primary-700 mb-1">Refund Amount</p>
        <p class="text-2xl font-bold text-primary-900">$850.00</p>
      </div>
    </div>
  </div>

  <!-- Refund Form Card -->
  <div class="bg-white rounded-xl shadow-md border border-gray-100 p-8">
    <form class="space-y-6">
      <!-- Bank Information Section -->
      <div>
        <h2 class="text-xl font-semibold text-gray-900 mb-6 flex items-center">
          <svg class="w-6 h-6 mr-3 text-primary-600" fill="none" stroke="currentColor">
            <!-- Bank building icon -->
          </svg>
          Bank Account Information
        </h2>

        <div class="space-y-5">
          <!-- Account Holder Name -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              Account Holder Name <span class="text-danger-500">*</span>
            </label>
            <input type="text" 
                   placeholder="Enter full name as it appears on your bank account"
                   class="block w-full px-4 py-3 
                          border border-gray-300 rounded-lg 
                          focus:ring-2 focus:ring-primary-500 focus:border-transparent 
                          transition-all duration-200
                          text-gray-900 placeholder-gray-400">
          </div>

          <!-- Bank Name -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              Bank Name <span class="text-danger-500">*</span>
            </label>
            <select class="block w-full px-4 py-3 
                           border border-gray-300 rounded-lg 
                           focus:ring-2 focus:ring-primary-500 focus:border-transparent 
                           transition-all duration-200
                           text-gray-900">
              <option value="">Select your bank</option>
              <option value="vietcombank">Vietcombank</option>
              <option value="techcombank">Techcombank</option>
              <option value="vietinbank">VietinBank</option>
              <option value="bidv">BIDV</option>
              <option value="acb">ACB</option>
              <option value="other">Other</option>
            </select>
          </div>

          <!-- Account Number -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              Account Number <span class="text-danger-500">*</span>
            </label>
            <input type="text" 
                   placeholder="Enter your bank account number"
                   class="block w-full px-4 py-3 
                          border border-gray-300 rounded-lg 
                          focus:ring-2 focus:ring-primary-500 focus:border-transparent 
                          transition-all duration-200
                          text-gray-900 placeholder-gray-400 
                          font-mono">
          </div>

          <!-- Routing Number / Bank Branch -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              Bank Branch / Routing Number <span class="text-danger-500">*</span>
            </label>
            <input type="text" 
                   placeholder="Enter bank branch or routing number"
                   class="block w-full px-4 py-3 
                          border border-gray-300 rounded-lg 
                          focus:ring-2 focus:ring-primary-500 focus:border-transparent 
                          transition-all duration-200
                          text-gray-900 placeholder-gray-400">
          </div>
        </div>
      </div>

      <!-- Security Notice -->
      <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div class="flex">
          <svg class="h-5 w-5 text-blue-600 mt-0.5 flex-shrink-0" fill="currentColor">
            <!-- Shield check icon -->
          </svg>
          <div class="ml-3">
            <h4 class="text-sm font-medium text-blue-800 mb-1">
              Your Information is Secure
            </h4>
            <p class="text-xs text-blue-700">
              All bank account information is encrypted and securely stored. 
              We will never share your details with third parties.
            </p>
          </div>
        </div>
      </div>

      <!-- Processing Timeline -->
      <div class="bg-gray-50 rounded-lg p-6">
        <h3 class="text-sm font-semibold text-gray-900 mb-4">
          Refund Processing Timeline
        </h3>
        <div class="space-y-3">
          <div class="flex items-start">
            <div class="flex-shrink-0 w-6 h-6 bg-primary-100 rounded-full 
                        flex items-center justify-center mt-0.5">
              <span class="text-xs font-bold text-primary-600">1</span>
            </div>
            <div class="ml-3">
              <p class="text-sm font-medium text-gray-900">Submission</p>
              <p class="text-xs text-gray-600">Your refund request is received immediately</p>
            </div>
          </div>
          <div class="flex items-start">
            <div class="flex-shrink-0 w-6 h-6 bg-primary-100 rounded-full 
                        flex items-center justify-center mt-0.5">
              <span class="text-xs font-bold text-primary-600">2</span>
            </div>
            <div class="ml-3">
              <p class="text-sm font-medium text-gray-900">Review (1-2 business days)</p>
              <p class="text-xs text-gray-600">Our team reviews your refund request</p>
            </div>
          </div>
          <div class="flex items-start">
            <div class="flex-shrink-0 w-6 h-6 bg-primary-100 rounded-full 
                        flex items-center justify-center mt-0.5">
              <span class="text-xs font-bold text-primary-600">3</span>
            </div>
            <div class="ml-3">
              <p class="text-sm font-medium text-gray-900">Processing (5-7 business days)</p>
              <p class="text-xs text-gray-600">Refund is processed to your bank account</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Terms Agreement -->
      <div>
        <label class="flex items-start cursor-pointer">
          <input type="checkbox" 
                 class="mt-1 rounded text-primary-600 
                        focus:ring-primary-500 focus:ring-offset-0">
          <span class="ml-3 text-sm text-gray-700">
            I confirm that the bank account information provided is accurate and 
            belongs to me. I understand that incorrect information may delay my refund.
          </span>
        </label>
      </div>

      <!-- Form Actions -->
      <div class="flex flex-col-reverse sm:flex-row sm:justify-end 
                  sm:space-x-3 space-y-3 space-y-reverse sm:space-y-0 pt-6">
        <button type="button" 
                class="w-full sm:w-auto inline-flex justify-center items-center 
                       px-6 py-3 
                       bg-white hover:bg-gray-50 
                       text-gray-700 font-medium rounded-lg 
                       border border-gray-300 
                       shadow-sm 
                       transition-all duration-200">
          Cancel
        </button>
        <button type="submit" 
                class="w-full sm:w-auto inline-flex justify-center items-center 
                       px-6 py-3 
                       bg-primary-600 hover:bg-primary-700 
                       text-white font-medium rounded-lg 
                       shadow-sm hover:shadow-md 
                       transition-all duration-200
                       transform hover:scale-105">
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor">
            <!-- Paper airplane icon -->
          </svg>
          Submit Refund Request
        </button>
      </div>
    </form>
  </div>

  <!-- Refund Status Tracker (Shown after submission) -->
  <div class="bg-white rounded-xl shadow-md border border-gray-100 p-8 mt-8 hidden" 
       id="refund-status-card">
    <h2 class="text-xl font-semibold text-gray-900 mb-6 flex items-center">
      <svg class="w-6 h-6 mr-3 text-primary-600" fill="none" stroke="currentColor">
        <!-- Clock icon -->
      </svg>
      Refund Request Status
    </h2>

    <!-- Status Progress Bar -->
    <div class="relative mb-8">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm font-medium text-primary-600">Pending Review</span>
        <span class="text-sm text-gray-500">Expected: 2 days</span>
      </div>
      <div class="w-full bg-gray-200 rounded-full h-2">
        <div class="bg-primary-600 h-2 rounded-full transition-all duration-500" 
             style="width: 33%"></div>
      </div>
    </div>

    <!-- Status Details -->
    <div class="bg-warning-50 border border-warning-200 rounded-lg p-4">
      <div class="flex">
        <svg class="h-5 w-5 text-warning-600 mt-0.5 animate-pulse" fill="currentColor">
          <!-- Clock icon -->
        </svg>
        <div class="ml-3">
          <p class="text-sm font-medium text-warning-800">
            Your refund request is pending review
          </p>
          <p class="text-xs text-warning-700 mt-1">
            We'll send you an email notification once your request has been reviewed. 
            Refund Reference: RF-2024-5678
          </p>
        </div>
      </div>
    </div>
  </div>
</div>
```



### 5. Invoice Preview & Download (UC22)

#### Invoice Preview Modal

```html
<div class="fixed inset-0 z-50 overflow-y-auto">
  <div class="flex items-center justify-center min-h-screen px-4 pt-4 pb-20">
    <!-- Backdrop -->
    <div class="fixed inset-0 bg-gray-900 bg-opacity-75 backdrop-blur-sm"></div>

    <!-- Modal Panel -->
    <div class="relative bg-white rounded-2xl shadow-2xl 
                max-w-4xl w-full max-h-[90vh] overflow-hidden">
      
      <!-- Modal Header -->
      <div class="bg-gradient-to-r from-primary-500 to-primary-600 px-6 py-4 
                  flex items-center justify-between">
        <h3 class="text-xl font-semibold text-white flex items-center">
          <svg class="w-6 h-6 mr-2" fill="none" stroke="currentColor">
            <!-- Document icon -->
          </svg>
          Invoice Preview
        </h3>
        <div class="flex items-center space-x-3">
          <button class="bg-white bg-opacity-20 hover:bg-opacity-30 
                         text-white rounded-lg px-4 py-2 
                         transition-colors duration-200 
                         flex items-center">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor">
              <!-- Download icon -->
            </svg>
            Download PDF
          </button>
          <button class="bg-white bg-opacity-20 hover:bg-opacity-30 
                         text-white rounded-lg p-2 
                         transition-colors duration-200">
            <svg class="w-5 h-5" fill="none" stroke="currentColor">
              <!-- X icon -->
            </svg>
          </button>
        </div>
      </div>

      <!-- Invoice Content (Scrollable) -->
      <div class="overflow-y-auto max-h-[calc(90vh-80px)] p-8 bg-gray-50">
        <div class="bg-white shadow-lg rounded-lg p-12 max-w-3xl mx-auto">
          <!-- Invoice HTML Content (see below) -->
        </div>
      </div>
    </div>
  </div>
</div>
```

#### Professional Invoice Layout

```html
<!-- Invoice Document -->
<div class="bg-white" id="invoice-content">
  <!-- Invoice Header -->
  <div class="flex items-start justify-between mb-8 pb-6 border-b-2 border-gray-200">
    <!-- Company Logo & Info -->
    <div>
      <img src="/logo.png" alt="Company Logo" class="h-12 mb-3">
      <h1 class="text-3xl font-bold text-gray-900 mb-2">MVH Tours</h1>
      <div class="text-sm text-gray-600 space-y-1">
        <p>123 Travel Street, District 1</p>
        <p>Ho Chi Minh City, Vietnam</p>
        <p>Phone: +84 28 1234 5678</p>
        <p>Email: info@mvhtours.com</p>
      </div>
    </div>

    <!-- Invoice Title & Number -->
    <div class="text-right">
      <h2 class="text-4xl font-bold text-primary-600 mb-2">INVOICE</h2>
      <div class="text-sm text-gray-600 space-y-1">
        <p class="font-medium text-gray-900">Invoice #: INV-2024-1234</p>
        <p>Issue Date: December 15, 2024</p>
        <p>Due Date: December 15, 2024</p>
      </div>
    </div>
  </div>

  <!-- Bill To Section -->
  <div class="mb-8">
    <h3 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
      Bill To
    </h3>
    <div class="bg-gray-50 rounded-lg p-4">
      <p class="font-semibold text-gray-900 text-lg mb-1">John Doe</p>
      <div class="text-sm text-gray-600 space-y-1">
        <p>Email: john.doe@example.com</p>
        <p>Phone: +84 123 456 789</p>
        <p>Booking Ref: BK-2024-1234</p>
      </div>
    </div>
  </div>

  <!-- Tour Details Section -->
  <div class="mb-8">
    <h3 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
      Tour Details
    </h3>
    <div class="bg-primary-50 border border-primary-200 rounded-lg p-4">
      <p class="font-semibold text-gray-900 text-lg mb-2">
        Ha Long Bay Premium Cruise Experience
      </p>
      <div class="grid grid-cols-2 gap-4 text-sm text-gray-700">
        <div>
          <span class="text-gray-500">Destination:</span>
          <span class="font-medium ml-2">Quang Ninh, Vietnam</span>
        </div>
        <div>
          <span class="text-gray-500">Duration:</span>
          <span class="font-medium ml-2">3 Days 2 Nights</span>
        </div>
        <div>
          <span class="text-gray-500">Departure:</span>
          <span class="font-medium ml-2">December 25, 2024</span>
        </div>
        <div>
          <span class="text-gray-500">Participants:</span>
          <span class="font-medium ml-2">2 Adults</span>
        </div>
      </div>
    </div>
  </div>

  <!-- Itemized Charges Table -->
  <div class="mb-8">
    <table class="w-full">
      <thead>
        <tr class="bg-gray-100 border-y border-gray-300">
          <th class="py-3 px-4 text-left text-sm font-semibold text-gray-700 uppercase">
            Description
          </th>
          <th class="py-3 px-4 text-right text-sm font-semibold text-gray-700 uppercase">
            Quantity
          </th>
          <th class="py-3 px-4 text-right text-sm font-semibold text-gray-700 uppercase">
            Unit Price
          </th>
          <th class="py-3 px-4 text-right text-sm font-semibold text-gray-700 uppercase">
            Amount
          </th>
        </tr>
      </thead>
      <tbody>
        <tr class="border-b border-gray-200">
          <td class="py-4 px-4">
            <p class="font-medium text-gray-900">Premium Cruise Package</p>
            <p class="text-sm text-gray-600">3D2N Ha Long Bay Tour</p>
          </td>
          <td class="py-4 px-4 text-right text-gray-900">2</td>
          <td class="py-4 px-4 text-right text-gray-900">$400.00</td>
          <td class="py-4 px-4 text-right font-medium text-gray-900">$800.00</td>
        </tr>
        <tr class="border-b border-gray-200">
          <td class="py-4 px-4">
            <p class="font-medium text-gray-900">Service Fee</p>
            <p class="text-sm text-gray-600">Booking & Processing</p>
          </td>
          <td class="py-4 px-4 text-right text-gray-900">1</td>
          <td class="py-4 px-4 text-right text-gray-900">$30.00</td>
          <td class="py-4 px-4 text-right font-medium text-gray-900">$30.00</td>
        </tr>
        <tr class="border-b border-gray-200">
          <td class="py-4 px-4">
            <p class="font-medium text-gray-900">Tax (VAT 10%)</p>
          </td>
          <td class="py-4 px-4 text-right text-gray-900">-</td>
          <td class="py-4 px-4 text-right text-gray-900">-</td>
          <td class="py-4 px-4 text-right font-medium text-gray-900">$20.00</td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- Total Section -->
  <div class="flex justify-end mb-8">
    <div class="w-80">
      <div class="space-y-2 mb-4">
        <div class="flex justify-between py-2 text-gray-700">
          <span>Subtotal:</span>
          <span class="font-medium">$830.00</span>
        </div>
        <div class="flex justify-between py-2 text-gray-700">
          <span>Tax (VAT 10%):</span>
          <span class="font-medium">$20.00</span>
        </div>
      </div>
      <div class="flex justify-between py-4 border-t-2 border-gray-300">
        <span class="text-xl font-bold text-gray-900">Total Amount:</span>
        <span class="text-2xl font-bold text-primary-600">$850.00</span>
      </div>
    </div>
  </div>

  <!-- Payment Information -->
  <div class="mb-8 bg-success-50 border border-success-200 rounded-lg p-4">
    <div class="flex items-center mb-3">
      <svg class="w-5 h-5 text-success-600 mr-2" fill="currentColor">
        <!-- Checkmark icon -->
      </svg>
      <h3 class="font-semibold text-success-900">Payment Completed</h3>
    </div>
    <div class="grid grid-cols-2 gap-4 text-sm text-gray-700">
      <div>
        <span class="text-gray-600">Payment Method:</span>
        <span class="font-medium ml-2">Visa •••• 4242</span>
      </div>
      <div>
        <span class="text-gray-600">Payment Date:</span>
        <span class="font-medium ml-2">December 15, 2024</span>
      </div>
      <div>
        <span class="text-gray-600">Transaction ID:</span>
        <span class="font-medium ml-2">TXN-2024-5678</span>
      </div>
      <div>
        <span class="text-gray-600">Status:</span>
        <span class="font-medium ml-2 text-success-700">Paid in Full</span>
      </div>
    </div>
  </div>

  <!-- Terms & Footer -->
  <div class="pt-6 border-t border-gray-200">
    <h4 class="font-semibold text-gray-900 text-sm mb-2">Terms & Conditions</h4>
    <p class="text-xs text-gray-600 leading-relaxed mb-4">
      This invoice is valid for the tour booking referenced above. Cancellation 
      policies apply as per our terms of service. For inquiries, please contact 
      our customer service team at support@mvhtours.com or call +84 28 1234 5678.
    </p>
    <div class="text-center text-xs text-gray-500 pt-4">
      <p>Thank you for choosing MVH Tours!</p>
      <p class="mt-1">www.mvhtours.com | Follow us on social media @mvhtours</p>
    </div>
  </div>
</div>
```



### 6. Toast Notifications

```html
<!-- Success Toast -->
<div class="fixed top-4 right-4 z-50 animate-slide-in-right">
  <div class="bg-white rounded-lg shadow-2xl border-l-4 border-success-500 
              p-4 max-w-md flex items-start">
    <div class="flex-shrink-0 bg-success-100 rounded-full p-2">
      <svg class="w-5 h-5 text-success-600" fill="currentColor">
        <!-- Checkmark icon -->
      </svg>
    </div>
    <div class="ml-3 flex-1">
      <h4 class="text-sm font-semibold text-gray-900 mb-1">Success!</h4>
      <p class="text-sm text-gray-600">Your booking has been cancelled successfully.</p>
    </div>
    <button class="ml-4 text-gray-400 hover:text-gray-600 transition-colors">
      <svg class="w-4 h-4" fill="currentColor">
        <!-- X icon -->
      </svg>
    </button>
  </div>
</div>

<!-- Error Toast -->
<div class="fixed top-4 right-4 z-50 animate-slide-in-right">
  <div class="bg-white rounded-lg shadow-2xl border-l-4 border-danger-500 
              p-4 max-w-md flex items-start">
    <div class="flex-shrink-0 bg-danger-100 rounded-full p-2">
      <svg class="w-5 h-5 text-danger-600" fill="currentColor">
        <!-- X circle icon -->
      </svg>
    </div>
    <div class="ml-3 flex-1">
      <h4 class="text-sm font-semibold text-gray-900 mb-1">Error</h4>
      <p class="text-sm text-gray-600">Unable to process cancellation. Please try again.</p>
    </div>
    <button class="ml-4 text-gray-400 hover:text-gray-600 transition-colors">
      <svg class="w-4 h-4" fill="currentColor">
        <!-- X icon -->
      </svg>
    </button>
  </div>
</div>

<!-- Info Toast -->
<div class="fixed top-4 right-4 z-50 animate-slide-in-right">
  <div class="bg-white rounded-lg shadow-2xl border-l-4 border-primary-500 
              p-4 max-w-md flex items-start">
    <div class="flex-shrink-0 bg-primary-100 rounded-full p-2">
      <svg class="w-5 h-5 text-primary-600" fill="currentColor">
        <!-- Info icon -->
      </svg>
    </div>
    <div class="ml-3 flex-1">
      <h4 class="text-sm font-semibold text-gray-900 mb-1">Information</h4>
      <p class="text-sm text-gray-600">Your invoice is being generated...</p>
    </div>
    <button class="ml-4 text-gray-400 hover:text-gray-600 transition-colors">
      <svg class="w-4 h-4" fill="currentColor">
        <!-- X icon -->
      </svg>
    </button>
  </div>
</div>
```

### Custom Animations (Tailwind Config)

```javascript
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      animation: {
        'slide-in-right': 'slideInRight 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'fade-in': 'fadeIn 0.2s ease-in',
      },
      keyframes: {
        slideInRight: {
          '0%': { transform: 'translateX(100%)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(20px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
      },
    },
  },
}
```

## Data Models

### Backend Data Models

#### Booking Entity

```java
@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_departure_date", columnList = "departure_date")
})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_reference", unique = true, nullable = false)
    private String bookingReference;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;
    
    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;
    
    @Column(name = "number_of_participants", nullable = false)
    private Integer numberOfParticipants;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;
    
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @OrderBy("timestamp DESC")
    private List<BookingStatusHistory> statusHistory;
    
    @OneToOne(mappedBy = "booking")
    private Refund refund;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
}
```



#### Other Key Entities

```java
@Entity
@Table(name = "booking_status_history")
public class BookingStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(length = 500)
    private String note;
}

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @Column(name = "transaction_reference", unique = true, nullable = false)
    private String transactionReference;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;
    
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
}

@Entity
@Table(name = "refunds")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @Column(name = "refund_reference", unique = true, nullable = false)
    private String refundReference;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;
    
    @Column(name = "bank_account_holder", nullable = false)
    private String bankAccountHolder;
    
    @Column(name = "bank_name", nullable = false)
    private String bankName;
    
    @Column(name = "account_number", nullable = false)
    @Convert(converter = SensitiveDataConverter.class)
    private String accountNumber;
    
    @Column(name = "routing_number", nullable = false)
    private String routingNumber;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}

@Entity
@Table(name = "tours")
public class Tour {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private String destination;
    
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;
    
    @Column(name = "duration_nights", nullable = false)
    private Integer durationNights;
    
    @Column(name = "price_per_person", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerPerson;
    
    @Column(name = "image_url")
    private String imageUrl;
}
```

#### Enums

```java
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

public enum RefundStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED
}

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}
```

### API DTOs

```java
public class BookingHistoryDTO {
    private Long id;
    private String bookingReference;
    private String tourName;
    private String destination;
    private LocalDate departureDate;
    private Integer durationDays;
    private Integer numberOfParticipants;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private String tourImageUrl;
}

public class BookingDetailDTO {
    private Long id;
    private String bookingReference;
    private BookingStatus status;
    private LocalDateTime createdAt;
    
    private TourInfoDTO tour;
    private CustomerInfoDTO customer;
    private PaymentInfoDTO payment;
    private List<StatusHistoryDTO> statusHistory;
    private RefundInfoDTO refund;
    
    private String cancellationReason;
    private LocalDateTime cancelledAt;
}

public class CancelBookingRequest {
    @NotBlank
    private String reason;
    
    private String additionalDetails;
}

public class RefundRequestDTO {
    @NotBlank
    private String bankAccountHolder;
    
    @NotBlank
    private String bankName;
    
    @NotBlank
    private String accountNumber;
    
    @NotBlank
    private String routingNumber;
}

public class InvoiceDataDTO {
    private String invoiceNumber;
    private LocalDate issueDate;
    private BookingDetailDTO booking;
    private List<InvoiceLineItem> lineItems;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
}
```

### Frontend Data Structures

```typescript
interface Booking {
  id: number;
  bookingReference: string;
  tourName: string;
  destination: string;
  departureDate: string;
  durationDays: number;
  numberOfParticipants: number;
  totalAmount: number;
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  createdAt: string;
  tourImageUrl?: string;
}

interface BookingDetail extends Booking {
  tour: TourInfo;
  customer: CustomerInfo;
  payment: PaymentInfo;
  statusHistory: StatusHistory[];
  refund?: RefundInfo;
  cancellationReason?: string;
  cancelledAt?: string;
}

interface FilterState {
  searchText: string;
  status: string[];
  dateFrom?: string;
  dateTo?: string;
  priceMin?: number;
  priceMax?: number;
}

interface PaginationState {
  currentPage: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
}
```



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, the following redundancies were identified and consolidated:

- **Authorization checks (2.7, 3.10, 4.11, 5.11, 6.3, 6.4)**: All represent the same authorization property - consolidated into Property 2
- **Authentication checks (6.1, 6.2, 6.5)**: All represent the same authentication requirement - consolidated into Property 1
- **Booking detail completeness (2.1, 2.2, 2.3, 2.4)**: Requirements 2.2-2.4 are subsumed by 2.1's "comprehensive information" - consolidated into Property 3
- **Invoice content (5.3-5.7)**: All PDF content requirements consolidated into single comprehensive property - Property 14
- **Refund creation (4.5, 12.2)**: Same property about initial refund status - consolidated into Property 10
- **UI conditional display**: Properties 3.1, 4.1, 5.1 are examples of UI behavior, kept as examples rather than properties
- **Status history ordering (2.5, 11.4)**: Same property - consolidated into Property 4
- **PDF generation success (5.8, 14.3)**: Same underlying property - consolidated into Property 14

### Property 1: Authentication Required

*For any* booking management API endpoint, unauthenticated requests should be rejected with a 401 Unauthorized error.

**Validates: Requirements 6.1, 6.2, 6.5**

### Property 2: Authorization Enforcement

*For any* authenticated customer attempting to access or modify a booking, the operation should succeed only if the booking belongs to that customer; otherwise a 403 Forbidden error should be returned.

**Validates: Requirements 2.7, 3.10, 4.11, 5.11, 6.3, 6.4**

### Property 3: Booking Detail Completeness

*For any* booking, the detail view should include all required fields: booking reference, tour information (name, description, duration, departure date, destination), customer information (name, email, phone, participants), payment details (amount, method, date, transaction reference), and status.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

### Property 4: Status History Chronological Ordering

*For any* booking with multiple status transitions, the status history should be ordered chronologically with the most recent status first.

**Validates: Requirements 2.5, 11.4**

### Property 5: Customer Booking Isolation

*For any* authenticated customer querying booking history, all returned bookings should belong exclusively to that customer and no other customer's bookings should be included.

**Validates: Requirements 1.1**

### Property 6: Pagination Correctness

*For any* page number and page size, the returned booking subset should contain exactly the specified number of items (or fewer on the last page), and the total count should accurately reflect the complete result set.

**Validates: Requirements 1.2**

### Property 7: Search Result Matching

*For any* search term, all returned bookings should contain that term in at least one of the searchable fields (tour name, booking reference, or destination).

**Validates: Requirements 1.3**

### Property 8: Filter Result Compliance

*For any* combination of filters (status, date range, price range), all returned bookings should match all active filter criteria.

**Validates: Requirements 1.4**

### Property 9: Statistics Calculation Accuracy

*For any* set of bookings, the calculated statistics (total bookings count, total amount spent, status distribution counts) should match the actual aggregated values from the booking set.

**Validates: Requirements 1.5**

### Property 10: Cancellation Business Rule - Started Tours

*For any* booking where the departure date is in the past (tour has started), cancellation requests should be rejected with an appropriate error message.

**Validates: Requirements 3.3**

### Property 11: Cancellation Business Rule - Terminal States

*For any* booking with status of Cancelled or Completed, cancellation requests should be rejected with an appropriate error message.

**Validates: Requirements 3.4**

### Property 12: Cancellation State Transition

*For any* booking with status Confirmed or Pending and a future departure date, successful cancellation should result in the booking status changing to Cancelled.

**Validates: Requirements 3.5**

### Property 13: Cancellation Metadata Recording

*For any* successfully cancelled booking, the cancellation reason and cancellation timestamp should be populated in the booking record.

**Validates: Requirements 3.6**

### Property 14: Refund Request Validation

*For any* refund request, the request should be rejected if any required bank account field (account holder name, bank name, account number, routing number) is missing or empty.

**Validates: Requirements 4.4**

### Property 15: Refund Creation with Pending Status

*For any* valid refund request on a cancelled booking, a new refund record should be created with Refund_Status of Pending and linked to the booking.

**Validates: Requirements 4.5, 4.6, 12.2**

### Property 16: Duplicate Refund Prevention

*For any* booking that already has a refund request, subsequent refund request attempts should be rejected with an appropriate error message.

**Validates: Requirements 4.9**

### Property 17: Invoice PDF Generation Success

*For any* booking with status Confirmed or Completed, invoice generation should succeed and produce a valid PDF file containing all required sections (company info, invoice metadata, booking details, customer info, payment info, itemized charges, totals).

**Validates: Requirements 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 14.3**

### Property 18: Invoice Generation Idempotency

*For any* booking, generating the invoice multiple times should produce byte-for-byte identical PDF output, demonstrating idempotent behavior.

**Validates: Requirements 14.5**

### Property 19: Invoice Graceful Degradation

*For any* booking with missing optional fields (e.g., billing address, additional notes), invoice generation should succeed without displaying null or empty values for those fields.

**Validates: Requirements 14.6**

### Property 20: Initial Booking Status

*For any* newly created booking, the initial status should be set to Pending.

**Validates: Requirements 11.2**

### Property 21: Status Transition Timestamps

*For any* status transition in booking or refund records, a timestamp should be recorded for that transition.

**Validates: Requirements 11.3, 12.3**

### Property 22: Invalid Status Transition Prevention

*For any* booking, attempting an invalid status transition (e.g., Cancelled to Confirmed, Completed to Pending) should be rejected with an appropriate error message.

**Validates: Requirements 11.5**

### Property 23: Database Error Handling

*For any* operation that encounters a database error (connection failure, constraint violation, timeout), the system should return an appropriate error response and log the detailed error information.

**Validates: Requirements 7.3**

### Property 24: Optimistic Locking for Concurrent Modifications

*For any* two concurrent attempts to modify the same booking, the second modification should detect the version conflict and fail with an optimistic locking error, ensuring data consistency.

**Validates: Requirements 7.4**



## Error Handling

### Error Response Structure

All API endpoints should return consistent error responses:

```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'accountNumber': must not be blank",
  "path": "/api/bookings/123/refund",
  "traceId": "a1b2c3d4-e5f6-7890"
}
```

### Error Categories

#### 1. Authentication Errors (401)

- Missing or invalid JWT token
- Expired authentication token
- Token signature verification failure

**Response**: `401 Unauthorized` with message "Authentication required"

#### 2. Authorization Errors (403)

- Attempting to access another customer's booking
- Attempting to perform admin-only operations

**Response**: `403 Forbidden` with message "You do not have permission to access this resource"

#### 3. Validation Errors (400)

- Missing required fields in request body
- Invalid data format (e.g., invalid date format)
- Business rule violations (e.g., cancelling completed booking)

**Response**: `400 Bad Request` with detailed validation messages

#### 4. Not Found Errors (404)

- Booking ID does not exist
- Invoice not available for given booking

**Response**: `404 Not Found` with message "Booking with ID {id} not found"

#### 5. Conflict Errors (409)

- Optimistic locking conflict (concurrent modification)
- Duplicate refund request
- Invalid state transition

**Response**: `409 Conflict` with specific conflict reason

#### 6. Server Errors (500)

- Database connection failures
- PDF generation errors
- Unexpected exceptions

**Response**: `500 Internal Server Error` with generic message "An unexpected error occurred. Please try again later."

**Important**: Detailed error information should be logged server-side but not exposed to clients for security reasons.

### Frontend Error Handling

#### API Error Interceptor

```javascript
async function handleApiError(error) {
  if (error.response) {
    switch (error.response.status) {
      case 401:
        // Redirect to login page
        window.location.href = '/login';
        break;
      case 403:
        showToast('error', 'You do not have permission to perform this action');
        break;
      case 404:
        showToast('error', 'The requested resource was not found');
        break;
      case 409:
        showToast('error', error.response.data.message || 'A conflict occurred');
        break;
      case 500:
        showToast('error', 'A server error occurred. Please try again later');
        break;
      default:
        showToast('error', error.response.data.message || 'An error occurred');
    }
  } else if (error.request) {
    // Network error
    showToast('error', 'Unable to connect to server. Please check your connection');
  } else {
    showToast('error', 'An unexpected error occurred');
  }
  
  // Log error for debugging
  console.error('API Error:', error);
}
```

#### Retry Logic for Transient Failures

```javascript
async function fetchWithRetry(url, options, maxRetries = 3) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await fetch(url, options);
    } catch (error) {
      if (attempt === maxRetries || !isRetriableError(error)) {
        throw error;
      }
      await delay(1000 * attempt); // Exponential backoff
    }
  }
}

function isRetriableError(error) {
  return error.response?.status >= 500 || !error.response;
}
```

### Logging Strategy

#### Backend Logging Levels

- **ERROR**: System failures, database errors, PDF generation failures
- **WARN**: Business rule violations, authorization failures, invalid state transitions
- **INFO**: Successful operations (booking cancelled, refund requested, invoice generated)
- **DEBUG**: Detailed request/response data for troubleshooting

#### Sensitive Data Protection

- Bank account numbers should be masked in logs (show only last 4 digits)
- Payment card numbers should never be logged
- Customer PII should be minimized in logs

```java
log.info("Refund request created for booking {} with masked account ending in {}", 
         bookingId, 
         maskAccountNumber(accountNumber));
```



## Testing Strategy

### Dual Testing Approach

The MVH Booking Management system requires both **unit testing** and **property-based testing** to ensure comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, integration points, and error conditions
- **Property tests**: Verify universal properties hold across all possible inputs
- Together: Unit tests catch concrete bugs, property tests verify general correctness

### Property-Based Testing Configuration

#### PBT Library Selection

**Java**: Use **jqwik** (recommended) or **QuickTheories**
- jqwik: Modern, annotation-based, excellent JUnit 5 integration
- Configuration: Minimum 100 iterations per property test

```java
// Example jqwik configuration
@Property
@PropertyDefaults(tries = 100)
```

#### Property Test Structure

Each correctness property from the design document must be implemented as a property-based test with:

1. **Arbitrary generators** for test data (bookings, customers, refunds)
2. **Property assertion** that checks the universal rule
3. **Test tag** referencing the design property

**Tag Format**:
```java
/**
 * Feature: mvh-booking-management
 * Property 5: Customer Booking Isolation
 * 
 * For any authenticated customer querying booking history, all returned 
 * bookings should belong exclusively to that customer
 */
@Property
@Label("Property 5: Customer Booking Isolation")
void customerOnlySeesOwnBookings(@ForAll Customer customer, 
                                  @ForAll List<Booking> allBookings) {
    // Test implementation
}
```

### Property Test Examples

#### Property 5: Customer Booking Isolation

```java
@Property
@Label("Property 5: Customer Booking Isolation")
void customerOnlySeesOwnBookings(
    @ForAll("customers") Customer customer,
    @ForAll("bookingLists") List<Booking> customerBookings,
    @ForAll("bookingLists") List<Booking> otherBookings
) {
    // Setup: Assign bookings to customer and others
    customerBookings.forEach(b -> b.setCustomer(customer));
    otherBookings.forEach(b -> b.setCustomer(new Customer()));
    
    bookingRepository.saveAll(customerBookings);
    bookingRepository.saveAll(otherBookings);
    
    // Execute: Fetch bookings for customer
    List<Booking> result = bookingService.getBookingHistory(customer.getId());
    
    // Assert: All returned bookings belong to customer
    assertThat(result).allMatch(b -> b.getCustomer().getId().equals(customer.getId()));
    assertThat(result).hasSameSizeAs(customerBookings);
}
```

#### Property 7: Search Result Matching

```java
@Property
@Label("Property 7: Search Result Matching")
void searchReturnsOnlyMatchingBookings(
    @ForAll("bookings") List<Booking> bookings,
    @ForAll @AlphaChars @StringLength(min = 3, max = 10) String searchTerm
) {
    // Setup
    bookingRepository.saveAll(bookings);
    
    // Execute
    List<Booking> results = bookingService.searchBookings(searchTerm);
    
    // Assert: All results contain search term in at least one field
    assertThat(results).allMatch(booking -> 
        booking.getTour().getName().contains(searchTerm) ||
        booking.getBookingReference().contains(searchTerm) ||
        booking.getTour().getDestination().contains(searchTerm)
    );
}
```

#### Property 12: Cancellation State Transition

```java
@Property
@Label("Property 12: Cancellation State Transition")
void validCancellationChangesStatusToCancelled(
    @ForAll("cancellableBooking") Booking booking,
    @ForAll @AlphaChars @StringLength(min = 5, max = 100) String reason
) {
    // Precondition: Booking is confirmed/pending and in future
    Assume.that(booking.getStatus() == BookingStatus.CONFIRMED || 
                booking.getStatus() == BookingStatus.PENDING);
    Assume.that(booking.getDepartureDate().isAfter(LocalDate.now()));
    
    // Execute
    bookingService.cancelBooking(booking.getId(), reason);
    
    // Assert
    Booking cancelled = bookingRepository.findById(booking.getId()).get();
    assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    assertThat(cancelled.getCancellationReason()).isEqualTo(reason);
    assertThat(cancelled.getCancelledAt()).isNotNull();
}
```

#### Property 18: Invoice Generation Idempotency

```java
@Property
@Label("Property 18: Invoice Generation Idempotency")
void invoiceGenerationIsIdempotent(
    @ForAll("confirmedBooking") Booking booking
) {
    // Execute: Generate invoice twice
    byte[] pdf1 = invoiceGenerator.generateInvoice(booking);
    byte[] pdf2 = invoiceGenerator.generateInvoice(booking);
    
    // Assert: PDFs are byte-for-byte identical
    assertThat(pdf1).isEqualTo(pdf2);
}
```

### Unit Test Coverage

#### Unit Test Focus Areas

1. **Specific Examples**:
   - Cancelling a booking 2 days before departure (should reject - cancellation policy)
   - Searching for "Ha Long" returns specific expected bookings
   - Creating a refund with all required fields succeeds

2. **Edge Cases**:
   - Empty booking list displays "no results" message
   - Searching with empty string returns all bookings
   - Pagination with page size larger than total items

3. **Error Conditions**:
   - Invalid booking ID returns 404
   - Missing JWT token returns 401
   - Duplicate refund request returns 409

4. **Integration Points**:
   - REST controller → Service layer integration
   - Service layer → Repository integration
   - PDF generation integration
   - Database transaction boundaries

#### Example Unit Tests

```java
@Test
void cancelBooking_withStartedTour_shouldRejectWithError() {
    // Arrange
    Booking pastBooking = createBooking(LocalDate.now().minusDays(1));
    
    // Act & Assert
    assertThatThrownBy(() -> 
        bookingService.cancelBooking(pastBooking.getId(), "Changed plans")
    )
    .isInstanceOf(BusinessRuleException.class)
    .hasMessageContaining("cannot be cancelled after it has started");
}

@Test
void getBookingDetail_withUnauthorizedCustomer_shouldReturn403() {
    // Arrange
    Long bookingId = 123L;
    Long ownerId = 1L;
    Long requesterId = 2L;
    
    when(bookingRepository.findById(bookingId))
        .thenReturn(Optional.of(createBooking(ownerId)));
    
    // Act & Assert
    assertThatThrownBy(() -> 
        bookingService.getBookingDetail(bookingId, requesterId)
    )
    .isInstanceOf(ForbiddenException.class)
    .hasMessageContaining("You do not have permission");
}

@Test
void calculateStatistics_withMixedStatuses_shouldReturnAccurateCounts() {
    // Arrange
    List<Booking> bookings = Arrays.asList(
        createBooking(BookingStatus.CONFIRMED, new BigDecimal("100")),
        createBooking(BookingStatus.CONFIRMED, new BigDecimal("200")),
        createBooking(BookingStatus.PENDING, new BigDecimal("150")),
        createBooking(BookingStatus.CANCELLED, new BigDecimal("300"))
    );
    
    // Act
    BookingStatistics stats = bookingService.calculateStatistics(bookings);
    
    // Assert
    assertThat(stats.getTotalBookings()).isEqualTo(4);
    assertThat(stats.getTotalSpent()).isEqualByComparingTo(new BigDecimal("750"));
    assertThat(stats.getConfirmedCount()).isEqualTo(2);
    assertThat(stats.getPendingCount()).isEqualTo(1);
    assertThat(stats.getCancelledCount()).isEqualTo(1);
}
```

### Frontend Testing

#### Component Tests (React Testing Library / Vitest)

```javascript
describe('BookingCard', () => {
  it('displays booking information correctly', () => {
    const booking = {
      tourName: 'Ha Long Bay Cruise',
      destination: 'Quang Ninh',
      status: 'CONFIRMED',
      totalAmount: 850
    };
    
    render(<BookingCard booking={booking} />);
    
    expect(screen.getByText('Ha Long Bay Cruise')).toBeInTheDocument();
    expect(screen.getByText('Quang Ninh')).toBeInTheDocument();
    expect(screen.getByText('Confirmed')).toBeInTheDocument();
    expect(screen.getByText('$850.00')).toBeInTheDocument();
  });
  
  it('shows cancel button only for confirmed/pending bookings', () => {
    const confirmedBooking = { ...mockBooking, status: 'CONFIRMED' };
    const { rerender } = render(<BookingCard booking={confirmedBooking} />);
    
    expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    
    const completedBooking = { ...mockBooking, status: 'COMPLETED' };
    rerender(<BookingCard booking={completedBooking} />);
    
    expect(screen.queryByText('Cancel Booking')).not.toBeInTheDocument();
  });
});
```

#### API Integration Tests

```javascript
describe('Booking API Integration', () => {
  it('fetches booking history with filters', async () => {
    const filters = { status: ['CONFIRMED'], priceMax: 1000 };
    
    const response = await bookingApi.getHistory(filters);
    
    expect(response.data).toBeInstanceOf(Array);
    expect(response.data.every(b => b.status === 'CONFIRMED')).toBe(true);
    expect(response.data.every(b => b.totalAmount <= 1000)).toBe(true);
  });
});
```

### Test Data Generators (Arbitraries)

```java
@Provide
Arbitrary<Customer> customers() {
    return Combinators.combine(
        Arbitraries.integers().greaterOrEqual(1),
        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50),
        Arbitraries.emails()
    ).as(Customer::new);
}

@Provide
Arbitrary<Booking> cancellableBooking() {
    return Combinators.combine(
        customers(),
        Arbitraries.of(BookingStatus.CONFIRMED, BookingStatus.PENDING),
        Arbitraries.dates().between(LocalDate.now().plusDays(1), 
                                     LocalDate.now().plusMonths(6))
    ).as((customer, status, date) -> {
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setStatus(status);
        booking.setDepartureDate(date);
        return booking;
    });
}

@Provide
Arbitrary<List<Booking>> bookingLists() {
    return Arbitraries.integers().between(0, 20)
        .flatMap(size -> 
            Arbitraries.just(new ArrayList<>())
                .map(list -> IntStream.range(0, size)
                    .mapToObj(i -> createRandomBooking())
                    .collect(Collectors.toList()))
        );
}
```

### Test Execution Requirements

- **Unit tests**: Run on every commit (fast feedback)
- **Property tests**: Run on every pull request (comprehensive validation)
- **Integration tests**: Run on pre-production deployment
- **Minimum coverage**: 80% code coverage for service layer
- **CI/CD**: Automated test execution in GitHub Actions / GitLab CI

### Manual Testing Checklist

- [ ] Mobile responsiveness (iPhone, Android, tablet)
- [ ] Cross-browser compatibility (Chrome, Firefox, Safari, Edge)
- [ ] Accessibility with screen readers
- [ ] PDF rendering in different PDF readers
- [ ] End-to-end booking cancellation flow
- [ ] End-to-end refund request flow
- [ ] Invoice download and print functionality



## REST API Endpoints

### Authentication

All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

### 1. Get Booking History (UC18)

```
GET /api/bookings
```

**Query Parameters**:
- `page` (integer, default: 0): Page number (0-indexed)
- `size` (integer, default: 10): Page size
- `search` (string, optional): Search term for tour name, booking reference, or destination
- `status` (array[string], optional): Filter by status (PENDING, CONFIRMED, COMPLETED, CANCELLED)
- `dateFrom` (string, optional): Filter by departure date from (ISO 8601)
- `dateTo` (string, optional): Filter by departure date to (ISO 8601)
- `priceMin` (number, optional): Filter by minimum price
- `priceMax` (number, optional): Filter by maximum price

**Response 200 OK**:
```json
{
  "content": [
    {
      "id": 123,
      "bookingReference": "BK-2024-1234",
      "tourName": "Ha Long Bay Premium Cruise",
      "destination": "Quang Ninh, Vietnam",
      "departureDate": "2024-12-25",
      "durationDays": 3,
      "numberOfParticipants": 2,
      "totalAmount": 850.00,
      "status": "CONFIRMED",
      "createdAt": "2024-12-14T10:00:00Z",
      "tourImageUrl": "/images/tours/halong-bay.jpg"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 24,
  "totalPages": 3,
  "statistics": {
    "totalBookings": 24,
    "totalSpent": 12450.00,
    "confirmedCount": 18,
    "pendingCount": 3,
    "completedCount": 2,
    "cancelledCount": 1
  }
}
```

### 2. Get Booking Detail (UC19)

```
GET /api/bookings/{bookingId}
```

**Path Parameters**:
- `bookingId` (integer): Booking ID

**Response 200 OK**:
```json
{
  "id": 123,
  "bookingReference": "BK-2024-1234",
  "status": "CONFIRMED",
  "createdAt": "2024-12-14T10:00:00Z",
  "tour": {
    "id": 45,
    "name": "Ha Long Bay Premium Cruise",
    "description": "Experience the breathtaking beauty...",
    "destination": "Quang Ninh, Vietnam",
    "durationDays": 3,
    "durationNights": 2,
    "departureDate": "2024-12-25",
    "imageUrl": "/images/tours/halong-bay.jpg",
    "includedServices": [
      "Luxury Cabin Accommodation",
      "All Meals Included",
      "Guided Cave Tours"
    ]
  },
  "customer": {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+84 123 456 789",
    "numberOfParticipants": 2
  },
  "payment": {
    "transactionReference": "TXN-2024-5678",
    "amount": 850.00,
    "paymentMethod": "Visa •••• 4242",
    "paymentDate": "2024-12-15T14:30:00Z",
    "status": "COMPLETED"
  },
  "statusHistory": [
    {
      "status": "CONFIRMED",
      "timestamp": "2024-12-15T14:30:00Z",
      "note": "Payment confirmed"
    },
    {
      "status": "PENDING",
      "timestamp": "2024-12-14T10:00:00Z",
      "note": "Booking created"
    }
  ],
  "refund": null,
  "cancellationReason": null,
  "cancelledAt": null
}
```

**Response 403 Forbidden**:
```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this booking",
  "path": "/api/bookings/123"
}
```

**Response 404 Not Found**:
```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Booking with ID 123 not found",
  "path": "/api/bookings/123"
}
```

### 3. Cancel Booking (UC20)

```
POST /api/bookings/{bookingId}/cancel
```

**Path Parameters**:
- `bookingId` (integer): Booking ID

**Request Body**:
```json
{
  "reason": "SCHEDULE_CONFLICT",
  "additionalDetails": "Family emergency requires travel change"
}
```

**Response 200 OK**:
```json
{
  "success": true,
  "message": "Booking cancelled successfully",
  "bookingReference": "BK-2024-1234",
  "cancelledAt": "2024-12-15T16:45:00Z",
  "refundEligibility": {
    "eligible": true,
    "refundAmount": 425.00,
    "refundPercentage": 50,
    "reason": "Cancellation made 10 days before departure"
  }
}
```

**Response 400 Bad Request** (Invalid state):
```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot cancel booking with status COMPLETED",
  "path": "/api/bookings/123/cancel"
}
```

**Response 400 Bad Request** (Tour started):
```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot cancel booking after tour has started",
  "path": "/api/bookings/123/cancel"
}
```

### 4. Request Refund (UC21)

```
POST /api/bookings/{bookingId}/refund
```

**Path Parameters**:
- `bookingId` (integer): Booking ID

**Request Body**:
```json
{
  "bankAccountHolder": "John Doe",
  "bankName": "Vietcombank",
  "accountNumber": "1234567890",
  "routingNumber": "VCB-HCM-BRANCH-001"
}
```

**Response 201 Created**:
```json
{
  "success": true,
  "message": "Refund request submitted successfully",
  "refundReference": "RF-2024-5678",
  "amount": 425.00,
  "status": "PENDING",
  "expectedProcessingDays": 7,
  "requestedAt": "2024-12-15T17:00:00Z"
}
```

**Response 400 Bad Request** (Not cancelled):
```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Refund can only be requested for cancelled bookings",
  "path": "/api/bookings/123/refund"
}
```

**Response 409 Conflict** (Duplicate refund):
```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "A refund request already exists for this booking",
  "path": "/api/bookings/123/refund"
}
```

### 5. Generate and Download Invoice (UC22)

```
GET /api/bookings/{bookingId}/invoice
```

**Path Parameters**:
- `bookingId` (integer): Booking ID

**Response 200 OK**:
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="invoice-BK-2024-1234.pdf"`
- Body: PDF binary data

**Response 400 Bad Request** (Invalid status):
```json
{
  "timestamp": "2024-12-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invoice is only available for confirmed or completed bookings",
  "path": "/api/bookings/123/invoice"
}
```

### 6. Get Invoice Preview Data

```
GET /api/bookings/{bookingId}/invoice/preview
```

**Path Parameters**:
- `bookingId` (integer): Booking ID

**Response 200 OK**:
```json
{
  "invoiceNumber": "INV-2024-1234",
  "issueDate": "2024-12-15",
  "booking": {
    "bookingReference": "BK-2024-1234",
    "tourName": "Ha Long Bay Premium Cruise",
    "departureDate": "2024-12-25",
    "duration": "3 Days 2 Nights",
    "destination": "Quang Ninh, Vietnam",
    "participants": 2
  },
  "customer": {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+84 123 456 789"
  },
  "lineItems": [
    {
      "description": "Premium Cruise Package",
      "quantity": 2,
      "unitPrice": 400.00,
      "amount": 800.00
    },
    {
      "description": "Service Fee",
      "quantity": 1,
      "unitPrice": 30.00,
      "amount": 30.00
    },
    {
      "description": "Tax (VAT 10%)",
      "quantity": 1,
      "unitPrice": 20.00,
      "amount": 20.00
    }
  ],
  "subtotal": 830.00,
  "tax": 20.00,
  "total": 850.00,
  "payment": {
    "method": "Visa •••• 4242",
    "transactionId": "TXN-2024-5678",
    "date": "2024-12-15",
    "status": "PAID"
  }
}
```

## Swagger/OpenAPI Documentation

All endpoints should be documented with Swagger annotations:

```java
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking Management", description = "Endpoints for managing customer bookings")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    @GetMapping
    @Operation(
        summary = "Get booking history",
        description = "Retrieve paginated booking history for authenticated customer with optional search and filters"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking history retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BookingHistoryResponse> getBookingHistory(
        @Parameter(description = "Page number (0-indexed)") 
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Page size") 
        @RequestParam(defaultValue = "10") int size,
        
        @Parameter(description = "Search term") 
        @RequestParam(required = false) String search
    ) {
        // Implementation
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(
        summary = "Cancel booking",
        description = "Cancel a confirmed or pending booking with a reason"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid booking state or tour already started"),
        @ApiResponse(responseCode = "403", description = "Not authorized to cancel this booking"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<CancelBookingResponse> cancelBooking(
        @Parameter(description = "Booking ID") 
        @PathVariable Long bookingId,
        
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Cancellation details",
            required = true
        )
        @RequestBody @Valid CancelBookingRequest request
    ) {
        // Implementation
    }
}
```

## Security Considerations

### Authentication & Authorization

1. **JWT Token Validation**: All endpoints validate JWT signature and expiration
2. **Customer Isolation**: Service layer enforces customer-booking ownership
3. **Role-Based Access**: Future admin endpoints require ADMIN role

### Data Protection

1. **Bank Account Encryption**: Account numbers encrypted at rest using AES-256
2. **PII Minimization**: Logs mask sensitive data (account numbers, payment cards)
3. **HTTPS Only**: All API traffic encrypted in transit
4. **SQL Injection Prevention**: Use parameterized queries via JPA
5. **XSS Prevention**: Frontend sanitizes user input before rendering

### Rate Limiting

```java
@RateLimiter(
    name = "bookingApi",
    fallbackMethod = "rateLimitFallback"
)
public BookingHistoryResponse getBookingHistory(...) {
    // Implementation
}
```

- 100 requests per minute per customer for read operations
- 10 requests per minute per customer for write operations (cancel, refund)

## Performance Optimization

### Database Indexes

```sql
CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_departure_date ON bookings(departure_date);
CREATE INDEX idx_bookings_customer_status ON bookings(customer_id, status);
```

### Caching Strategy

- **Customer booking count**: Cache for 5 minutes
- **Tour information**: Cache for 1 hour (tours rarely change)
- **Invoice PDFs**: Cache generated PDFs for 24 hours

```java
@Cacheable(value = "booking-statistics", key = "#customerId")
public BookingStatistics getStatistics(Long customerId) {
    // Expensive calculation
}
```

### N+1 Query Prevention

```java
@Query("SELECT b FROM Booking b " +
       "LEFT JOIN FETCH b.tour " +
       "LEFT JOIN FETCH b.customer " +
       "LEFT JOIN FETCH b.payment " +
       "WHERE b.customer.id = :customerId")
List<Booking> findByCustomerIdWithDetails(@Param("customerId") Long customerId);
```

## Deployment Considerations

### Environment Configuration

```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

invoice:
  generator:
    temp-directory: /var/tmp/invoices
    cache-ttl: 86400 # 24 hours

booking:
  cancellation:
    cutoff-hours: 72 # 3 days before departure
```

### Monitoring & Observability

- **Metrics**: Prometheus metrics for API latency, error rates, booking operations
- **Logging**: Centralized logging with ELK stack
- **Tracing**: Distributed tracing with trace IDs in all logs
- **Alerts**: Alert on high error rates, PDF generation failures, database connection issues

---

## Summary

This design document specifies a modern, responsive booking management system with:

- **Beautiful UI**: Gradient cards, smooth animations, professional invoice layout
- **Comprehensive Testing**: 24 correctness properties with property-based testing + unit tests
- **Security**: JWT authentication, authorization enforcement, data encryption
- **Performance**: Database indexes, caching, optimized queries
- **Error Handling**: Consistent error responses, graceful degradation
- **Developer Experience**: Swagger documentation, clear separation of concerns

The implementation should follow the component specifications, UI designs, and testing strategy outlined in this document to deliver a high-quality booking management feature.

