package lv.pawsitter.controller;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lv.pawsitter.dto.BookingResponse;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.exception.InvalidBookingOperationException;
import lv.pawsitter.service.BookingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class StripeController
{
//********** VARS
    //base url for stripe payment redirection after payment, to use on localhost - http::localhost , and on production - real url
    @Value("${app.base-url}")
    private String baseUrl;

    private final BookingService bookingService;
    private final String webhookSecret;// a string variable that will hold the stripe webhook secret

    //constructor
    public StripeController(BookingService bookingService, @Value("${stripe.secret-key}") String stripeSecretKey, @Value("${stripe.webhook-secret}") String webhookSecret)
    {
        this.bookingService = bookingService;
        this.webhookSecret = webhookSecret;
        Stripe.apiKey = stripeSecretKey;
    }

    //CHECKOUT PAGE
    @PostMapping("/owner/bookings/{id}/checkout")
    public String createCheckout(@PathVariable Long id, Authentication authentication) throws StripeException
    {
        //get booking
        BookingResponse booking = bookingService.getBookingById(id, authentication.getName());

        //Validate Statuses
        if (booking.status() != BookingStatus.ACCEPTED)
        {
            throw new InvalidBookingOperationException("Only accepted bookings can be paid");
        }

        if (booking.paid())
        {
            throw new InvalidBookingOperationException("This booking has already been paid");
        }

        //calculate how many days are booked start -> end + 1
        long numberOfDays = ChronoUnit.DAYS.between(booking.startDate().toLocalDate(), booking.endDate().toLocalDate()) + 1;
        BigDecimal totalPrice = booking.pricePerDaySnapshot().multiply(BigDecimal.valueOf(numberOfDays));//BigDecimal because price is BigDecimal :)
        long amountInCents = totalPrice.movePointRight(2).longValueExact();

        //STRIPE PARAMS -> send price, succes and cancel page etc.
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(baseUrl + "/stripe/success") // localhost + production success and cancel pages
                        .setCancelUrl(baseUrl + "/owner/bookings/" + id)
                        .putMetadata("bookingId", id.toString())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams
                                                                    .LineItem.PriceData.ProductData.builder()
                                                                    .setName("PawSitter booking #" + id)
                                                                    .build()
                                                    )
                                                    .build()
                                )
                                .build()
                        )
                        .build();

        //sends the prepared Checkout Session settings in params to Stripe.
        Session session = Session.create(params);

        //redirects to Stripe payment page
        return "redirect:" + session.getUrl();
    }


    //GETTER FOR SUCCESS PAGE
    //runs after stripe redirects the user back to your application following a successful checkout(if success).
    @GetMapping("/stripe/success")
    public String paymentSuccess()
    {
        return "payment/paymentSuccess";
    }

    //webhook - so basicly that what we get as a callback from Stripe on success, and the
    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature)
    {
        // Rebuild and verify the Stripe event using the raw request body,
        // the Stripe-Signature header, and  webhook secret.
        Event event;
        try
        {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        }
        catch (SignatureVerificationException exception)
        {
            // Validations -  This prevents fake requests from marking bookings as paid.
            return ResponseEntity.badRequest().body("Invalid Stripe signature");
        }

        if ("checkout.session.completed".equals(event.getType()))
        {
            Session session;

            try
            {
                // Convert the general Stripe event data into a Checkout Session object.
                //  .deserializeUnsafe(); //  forces Stripe Java to deserialize the event anyway
                session = (Session) event.getDataObjectDeserializer().deserializeUnsafe();
            }
            catch (Exception exception)
            {
                //  Validations -  if the Checkout Session data cannot be read.
                return ResponseEntity.badRequest().body("Cannot deserialize Checkout Session");
            }

            if (session == null)
            {
                // Validations - Ensure that the event actually contains a Checkout Session.
                return ResponseEntity.badRequest().body("Missing checkout session");
            }

            //get booking id from metadata
            String bookingId = session.getMetadata().get("bookingId");

            //update status -> set is paid
            if (bookingId != null)
            {
                bookingService.confirmPayment(Long.valueOf(bookingId), session.getId());
            }
        }

        //sends an HTTP 200 OK response back to Stripe. ( successfully received event. Do not retry it.)
        return ResponseEntity.ok("Webhook received");
    }
}