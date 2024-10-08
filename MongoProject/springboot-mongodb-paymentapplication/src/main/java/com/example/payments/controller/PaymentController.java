package com.example.payments.controller;

import com.example.payments.dto.Paymentdto;
import com.example.payments.model.Payment;
import com.example.payments.service.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<Payment> initiatePayment(@RequestBody @Valid Paymentdto payment) {
        return new ResponseEntity<>(paymentService.initiatePayment(payment), HttpStatus.OK);
    }
    @PostMapping("/bulk-initiate")
    public ResponseEntity<List<Payment>> initiatePayments(@RequestBody @Valid List<Paymentdto> payments) {
        return new ResponseEntity<>(paymentService.initiatePayments(payments), HttpStatus.OK);
    }
    // 1. Endpoint to find pending payments
    @GetMapping("/pending")
    public ResponseEntity<List<Payment>> findPendingPayments() {
        return new ResponseEntity<>(paymentService.findPendingPayments(), HttpStatus.OK);
    }

    // 2. Endpoint to find total amount
    @GetMapping("/total-amount")
    public ResponseEntity<Double> getTotalAmount() {
        return new ResponseEntity<>(paymentService.getTotalAmount(), HttpStatus.OK);
    }

    // 3. Endpoint to find amount by invoice number
    @GetMapping("/amount/{invoiceNumber}")
    public ResponseEntity<Double> getAmountByInvoiceNumber(@PathVariable String invoiceNumber) {
        return new ResponseEntity<>(paymentService.getAmountByInvoiceNumber(invoiceNumber), HttpStatus.OK);
    }

    // 4. Endpoint to find complete and pending payments by date
    @GetMapping("/status-by-date/{paymentDate}")
    public ResponseEntity<Map<String, List<Payment>>> getPaymentsByStatusAndDate(@PathVariable String paymentDate) {
        return new ResponseEntity<>(paymentService.getPaymentsByStatusAndDate(paymentDate), HttpStatus.OK);
    }

    // 5. Endpoint to edit payment
    @PutMapping("/edit/{id}")
    public ResponseEntity<Payment> editPayment(@PathVariable String id, @RequestBody Paymentdto paymentdto) {
        return new ResponseEntity<>(paymentService.editPayment(id, paymentdto), HttpStatus.OK);
    }

    // 6. Endpoint to delete payment
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable String id) {
        paymentService.deletePayment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/netamount/{invoiceNumber}")
    public ResponseEntity<Double> getNetAmountByInvoiceNumber(@PathVariable("invoiceNumber") String invoiceNumber)
    {
        return new ResponseEntity<>(paymentService.getNetAmountByInvoiceNumber(invoiceNumber), HttpStatus.OK);
    }
//for pdf
@GetMapping("/invoice/{invoiceNumber}")
public void generateInvoice(@PathVariable String invoiceNumber, HttpServletResponse response) throws IOException, JRException {
    Payment payment = paymentService.getPaymentByInvoiceNumber(invoiceNumber); // Implement this method
    if (payment == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invoice not found");
        return;
    }

    // Prepare the data
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("invoiceNumber", payment.getInvoicenumber());
    parameters.put("paymentDate", payment.getPaymentdate());
    parameters.put("vendorName", payment.getUsername());

    // Load the Jasper template
    InputStream reportStream = getClass().getResourceAsStream("/templates/invoiceTemplate.jasper");
    JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
    JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(List.of(payment));

    // Generate the PDF
    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

    // Set response headers and content type
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "attachment; filename=invoice.pdf");

    // Write the PDF to the response output stream
    JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());
}

}
