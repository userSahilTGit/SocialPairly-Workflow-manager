package com.SocialPairly_Workflow_Manager.util;

import org.springframework.web.util.HtmlUtils;

import java.util.List;

public final class EmailTemplateBuilder {

    private static final String BRAND_GRADIENT = "linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%)";

    private EmailTemplateBuilder() {
    }

    public static String build(String appName, String supportEmail, String bodyContent) {
        String safeAppName = escape(appName);
        String safeSupportEmail = escape(supportEmail);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f4f8;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f4f8;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%%;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 8px 24px rgba(99,102,241,0.12);">
                          <tr>
                            <td style="background:%s;padding:28px 32px;text-align:center;">
                              <h1 style="margin:0;font-size:26px;font-weight:700;color:#ffffff;letter-spacing:0.5px;">%s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px 36px 28px;font-size:16px;line-height:1.65;color:#374151;">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 36px 32px;">
                              <hr style="border:none;border-top:1px solid #e5e7eb;margin:0 0 16px;">
                              <p style="margin:0;font-size:13px;color:#9ca3af;text-align:center;line-height:1.5;">
                                Questions? Reply to this email or contact
                                <a href="mailto:%s" style="color:#6366f1;text-decoration:none;">%s</a>
                              </p>
                              <p style="margin:12px 0 0;font-size:12px;color:#d1d5db;text-align:center;">
                                &copy; %s. All rights reserved.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(safeAppName, BRAND_GRADIENT, safeAppName, bodyContent, safeSupportEmail, safeSupportEmail, safeAppName);
    }

    public static String paragraph(String text) {
        return "<p style=\"margin:0 0 16px;\">" + escape(text) + "</p>";
    }

    public static String heading(String text) {
        return "<h2 style=\"margin:0 0 16px;font-size:20px;font-weight:600;color:#111827;\">" + escape(text) + "</h2>";
    }

    public static String button(String label, String url) {
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" style="margin:24px auto;">
                  <tr>
                    <td align="center" style="border-radius:8px;background:%s;">
                      <a href="%s" target="_blank" style="display:inline-block;padding:14px 28px;font-size:16px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:8px;">
                        %s
                      </a>
                    </td>
                  </tr>
                </table>
                """.formatted(BRAND_GRADIENT, escapeUrl(url), escape(label));
    }

    public static String infoBox(String title, List<String> lines) {
        StringBuilder rows = new StringBuilder();
        for (String line : lines) {
            rows.append("<tr><td style=\"padding:6px 0;font-size:15px;color:#374151;\">")
                    .append(escape(line))
                    .append("</td></tr>");
        }
        return """
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:20px 0;background-color:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;">
                  <tr>
                    <td style="padding:16px 20px;">
                      <p style="margin:0 0 10px;font-size:15px;font-weight:600;color:#6366f1;">%s</p>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        %s
                      </table>
                    </td>
                  </tr>
                </table>
                """.formatted(escape(title), rows);
    }

    public static String featureList(List<String> features) {
        StringBuilder items = new StringBuilder();
        for (String feature : features) {
            items.append("<li style=\"margin-bottom:8px;\">").append(escape(feature)).append("</li>");
        }
        return "<ul style=\"margin:12px 0 16px;padding-left:20px;color:#374151;\">" + items + "</ul>";
    }

    public static String highlightBox(String content) {
        return """
                <div style="margin:20px 0;padding:16px 20px;background:linear-gradient(135deg,#eef2ff 0%%,#fdf2f8 100%%);border-left:4px solid #8b5cf6;border-radius:6px;">
                  %s
                </div>
                """.formatted(content);
    }

    public static String refundSummaryTable(String originalAmount, String deductedAmount, String finalAmount, String deductionReason) {
        return refundSummaryTable(originalAmount, deductedAmount, null, finalAmount, deductionReason);
    }

    public static String refundSummaryTable(String originalAmount,
                                            String processingFeeAmount,
                                            String tokenUsageDeductionAmount,
                                            String finalAmount,
                                            String deductionReason) {
        String tokenRow = "";
        if (tokenUsageDeductionAmount != null && !tokenUsageDeductionAmount.isBlank()) {
            tokenRow = """
                  <tr>
                    <td style="padding:12px 16px;border-bottom:1px solid #e5e7eb;">Plan Token Usage Deduction</td>
                    <td style="padding:12px 16px;border-bottom:1px solid #e5e7eb;text-align:right;color:#dc2626;">-%s</td>
                  </tr>
                """.formatted(tokenUsageDeductionAmount);
        }
        return """
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:20px 0;border-collapse:collapse;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;">
                  <tr style="background-color:#6366f1;">
                    <th style="padding:12px 16px;text-align:left;color:#ffffff;font-size:14px;">Item</th>
                    <th style="padding:12px 16px;text-align:right;color:#ffffff;font-size:14px;">Amount</th>
                  </tr>
                  <tr>
                    <td style="padding:12px 16px;border-bottom:1px solid #e5e7eb;">Original Payment Amount</td>
                    <td style="padding:12px 16px;border-bottom:1px solid #e5e7eb;text-align:right;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:12px 16px;border-bottom:1px solid #e5e7eb;">Deductions (Processing/Policy Fees)</td>
                    <td style="padding:12px 16px;border-bottom:1px solid #e5e7eb;text-align:right;color:#dc2626;">-%s</td>
                  </tr>
                  %s
                  <tr style="background-color:#f0fdf4;">
                    <td style="padding:14px 16px;font-weight:700;">Total Refund Amount</td>
                    <td style="padding:14px 16px;text-align:right;font-weight:700;color:#059669;">%s</td>
                  </tr>
                </table>
                <p style="margin:0 0 16px;font-size:14px;color:#6b7280;line-height:1.5;">
                  <strong>Note:</strong> %s
                </p>
                """.formatted(originalAmount, processingFeeAmount, tokenRow, finalAmount, escape(deductionReason));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(value);
    }

    private static String escapeUrl(String url) {
        if (url == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(url);
    }
}
