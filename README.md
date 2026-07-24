# PawSitter

PawSitter is a web application that connects pet owners with pet sitters. Owners can search for available sitters, manage pets, 
create bookings, make payments, and view booking history. Sitters can manage their profiles, availability, and booking requests.

## How to Run the Project Locally

1. Rename `.env_test` to `.env`.
2. Fill in the missing environment variables. The test email configuration is already provided.
3. In `compose.yml`, uncomment the MySQL port mapping:  yaml ports: - "3306:3306"
4. Install and start Docker.
5. Open the project in IntelliJ IDEA.
6. Press Alt + F12 to open the terminal.
7. Start the MySQL container: run command docker compose up -d mysql ( do this after port uncoment and .env setup , or run docker compose down first)
8. Wait approximately 10 seconds for MySQL to start.
9. Run the Spring Boot application from IntelliJ or build into docker
10. For the Stripe local payment you will also need additional setup. like stripe cli, and both keys secret and for hook (or mark payment true in db manually)

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
- Technically, an owner can book all of a sitter’s available dates, which automatically unpublishes the sitter’s profile. If the booking is later cancelled, the dates are restored, but the profile remains unpublished and must be published again manually.


## Known Technical Debt and Future Improvements

The following improvements were identified during the final code review and presentation:

- Clean up the home controller and remove duplicated logic.
- Remove redundant controller code where `petService.getOwnerPet(authentication.getName(), id)` already returns a DTO.
- Improve Stripe Checkout error handling. Some errors currently return JSON instead of a user-friendly error message or error page.
- Clean up `PetRestController` and `PetServiceImpl`, as several methods and pieces of logic are no longer used.

- Use DTOs consistently across the application instead of returning entities directly.
- Ensure entities are mapped to DTOs before being returned from services or controllers.
- Use one consistent mapping approach across the project for both DTO-to-entity and entity-to-DTO conversion.
- Extend the existing converter or mapper structure so it supports all required entities and DTOs.

- Review the booking flow for cases where an owner requests bookings with multiple sitters for the same dates. Currently, more than one booking can exist for the same period. A future improvement would automatically cancel the other overlapping requests once one booking is accepted.
- Review the booking update flow and verify that availability, pet associations, prices, statuses, and validation are updated correctly after a booking request is changed.
