package com.pgapp.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pgapp.model.Owner;
import com.pgapp.model.Pg;
import com.pgapp.repository.OwnerRepository;
import com.pgapp.service.SubscriptionService;
import com.pgapp.util.PdfGenerator;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;

@Controller
public class SubscriptionController {
	@Value("${app.testing}")
	private boolean isTesting;
    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private JavaMailSender mailSender;

    // ================= Subscription Pages =================

    @GetMapping("/subscribe")
    public String showSubscriptionPage() {
        return "subscription"; 
    }

    @GetMapping("/subscribe/standard")
    public String chooseStandardPlan(HttpSession session) {
        session.setAttribute("selectedPlan", "STANDARD");
        session.setAttribute("planDurationDays", 365);
        return "redirect:/payment";
    }

    @GetMapping("/payment")
    public String showPaymentOptions(HttpSession session) {

        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId == null) {
            return "redirect:/login";
        }

        Owner owner = ownerRepository.findById(ownerId).orElse(null);
        if (owner != null) {
            session.setAttribute("ownerName", owner.getOwnerName());
            session.setAttribute("ownerEmail", owner.getEmail());
            session.setAttribute("ownerMobile", owner.getMobile());
        }

        return "payment";
    }

    // ================= Razorpay Order API =================

    @PostMapping("/createOrder")
    @ResponseBody
    public Map<String, Object> createOrder() throws Exception {
        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        int amount = 399 * 100; // ₹399 → paise

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "PGAPP_ORDER_101");

        Order order = razorpay.orders.create(orderRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.get("id"));
        response.put("razorpayKeyId", razorpayKeyId);
        response.put("amount", amount);

        return response;
    }

    // ================= Payment Success =================

    @GetMapping("/payment/success")
    public String paymentSuccess(HttpSession session) {

        Long ownerId = (Long) session.getAttribute("ownerId");
        if (ownerId != null) {
            Owner owner = ownerRepository.findById(ownerId).orElse(null);
            if (owner != null) {
                LocalDate today = LocalDate.now();
                Integer planDays = (Integer) session.getAttribute("planDurationDays");

                // Update subscription details
                owner.setSubscribed(true);
                owner.setSubscriptionPlan((String) session.getAttribute("selectedPlan"));
                owner.setSubscriptionStart(today);
                owner.setSubscriptionEnd(today.plusDays(planDays));

                ownerRepository.save(owner);

                // Send PDF email confirmation
                sendSubscriptionEmail(owner);
            }
        }

        return "redirect:/login?paymentSuccess=true";
    }

    // ================= Email with PDF Attachments =================

    private void sendSubscriptionEmail(Owner owner) {
        try {
            if (isTesting) {
                // ===== TEST MODE: Save PDFs locally =====
                if (owner.getPgs() != null && !owner.getPgs().isEmpty()) {
                    for (Pg pg : owner.getPgs()) {
                        byte[] pdfBytes = PdfGenerator.generateSubscriptionPdfForPG(owner, pg);
                        String fileName = "test_" + pg.getPgName().replaceAll("\\s+", "") + "_Subscription.pdf";
                        java.nio.file.Files.write(java.nio.file.Paths.get(fileName), pdfBytes);
                        System.out.println("PDF generated for testing: " + fileName);
                    }
                }
            } else {
                // ===== LIVE MODE: Send email with PDFs =====
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);

                helper.setTo(owner.getEmail());
                helper.setSubject("PG Subscription Confirmation");
                helper.setText(
                        "Dear " + owner.getOwnerName() + ",\n\n"
                      + "Thank you for subscribing! Please find the subscription details attached.\n\n"
                      + "Best Regards,\n"
                      + "Nivora Team\n"
                      + "PG & Hostel Management Platform\n"
                      + "https://nivora.work/"
                );

                if (owner.getPgs() != null && !owner.getPgs().isEmpty()) {
                    for (Pg pg : owner.getPgs()) {
                        byte[] pdfBytes = PdfGenerator.generateSubscriptionPdfForPG(owner, pg);
                        String fileName = pg.getPgName().replaceAll("\\s+", "") + "_Subscription.pdf";
                        helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));
                    }
                }

                mailSender.send(message);
                System.out.println("Subscription email sent to " + owner.getEmail());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}