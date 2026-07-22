# PawSitter

PawSitter is a web application that connects pet owners with pet sitters. Owners can search for available sitters, manage pets, 
create bookings, make payments, and view booking history. Sitters can manage their profiles, availability, and booking requests.

## Technical Features

- Role-based registration and authentication for owners, sitters, and administrators
- Sitter profiles with images, location, description, pricing, and availability
- Search by city, dates, maximum price, and partial availability
- Pet and booking management
- Booking acceptance, cancellation, payment, and completion
- Password recovery by email with expiring tokens
- Stripe Checkout integration
- Built with Spring Boot, Thymeleaf, Spring Security, JPA, MySQL, Maven, and Docker

## Demo and Current Limitations

- Demo accounts use simple passwords to make testing easier.
- To test payments locally, Stripe CLI must be installed and used to forward webhook events to the local application.
- Paid bookings can be marked as completed at any time to demonstrate the full workflow. In a production application, this action would only be available after the booking end date.
- Payments go only to the PawSitter platform account. Sitter onboarding, connected accounts, payouts, and commission handling are outside the project scope.
- Requested dates are removed from sitter availability immediately and restored after cancellation. This means a user could temporarily block a large date range. Reserving dates only after payment would require a larger redesign of booking statuses, pet associations, cancellation history, and payment expiration.
- Cancelled bookings remain stored with the `CANCELLED` status so users can view their booking history.
- Pets linked to previous bookings should not be permanently deleted. A future version could use an `active` flag to hide inactive pets from new bookings while preserving historical booking data.