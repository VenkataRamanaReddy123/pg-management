package com.pgapp.util;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.itextpdf.html2pdf.HtmlConverter;
import com.pgapp.model.Owner;
import com.pgapp.model.Pg;

public class PdfGenerator {

    public static byte[] generateSubscriptionPdfForPG(Owner owner, Pg pg) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Convert date to DD-MM-YYYY
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String paymentDate = LocalDate.now().format(formatter);
            String startDate = owner.getSubscriptionStart() != null ? owner.getSubscriptionStart().format(formatter) : "-";
            String endDate = owner.getSubscriptionEnd() != null ? owner.getSubscriptionEnd().format(formatter) : "-";

            String html = "<html><head>"
                    + "<style>"
                    + "body { font-family: Arial, sans-serif; margin: 0; padding: 0; }"

                    /* Header */
                    + ".header { background: linear-gradient(90deg, #512da8, #9575cd); "
                    + "color: white; padding: 25px; text-align: center; }"
                    + ".header h1 { margin: 0; font-size: 28px; }"

                    /* Content */
                    + ".content { padding: 25px; font-size: 15px; }"
                    + "span.owner-name { color: #ff9800; font-weight: bold; font-size: 17px; }"

                    /* ENTIRE TABLE SAME COLOR */
                    + "table { width: 100%; border-collapse: collapse; margin-top: 25px; "
                    + "border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }"

                    + "th { background: #512da8; color: white; padding: 12px; font-size: 15px; }"
                    + "td { background: #f3e5f5; padding: 12px; font-size: 14px; border-bottom: 1px solid #ddd; }"

                    + "</style>"
                    + "</head><body>"

                    + "<div class='header'><h1>PG Subscription Confirmation</h1></div>"

                    + "<div class='content'>"
                    + "<p>Dear <span class='owner-name'>" + owner.getOwnerName() + "</span>,</p>"
                    + "<p>Thank you for subscribing. Below are your subscription details:</p>"

                    + "<table>"
                    + "<tr><th>PG Name</th><td>" + pg.getPgName() + "</td></tr>"
                    + "<tr><th>Subscription Plan</th><td>" + owner.getSubscriptionPlan() + "</td></tr>"
                    + "<tr><th>Payment Date</th><td>" + paymentDate + "</td></tr>"
                    + "<tr><th>Owner Mobile</th><td>" + owner.getMobile() + "</td></tr>"
                    + "<tr><th>Start Date</th><td>" + startDate + "</td></tr>"
                    + "<tr><th>End Date</th><td>" + endDate + "</td></tr>"
                    + "</table>"

                    + "<p style='margin-top:25px;'>Best Regards,<br><b>PG App Team</b></p>"
                    + "</div>"

                    + "</body></html>";

            HtmlConverter.convertToPdf(html, out);
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
