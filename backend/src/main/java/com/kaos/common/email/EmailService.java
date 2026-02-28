package com.kaos.common.email;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio genérico de envío de correo HTML.
 *
 * <p>Se habilita via {@code kaos.email.habilitado=true} en application.yml.
 * Si está deshabilitado, {@link #enviarHtml} registra solo un aviso.</p>
 *
 * <p>Requiere {@code spring-boot-starter-mail} y configuración SMTP en
 * {@code spring.mail.*}.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${kaos.email.habilitado:false}")
    private boolean habilitado;

    @Value("${spring.mail.username:noreply@kaos.local}")
    private String remitente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo HTML a una lista de destinatarios.
     *
     * <p>Si {@code kaos.email.habilitado=false} el envío se omite silenciosamente.</p>
     *
     * @param destinatarios lista de direcciones destino
     * @param asunto        asunto del correo
     * @param cuerpoHtml    contenido HTML del correo
     */
    public void enviarHtml(List<String> destinatarios, String asunto, String cuerpoHtml) {
        if (!habilitado) {
            log.debug("[EmailService] Email deshabilitado (kaos.email.habilitado=false). Asunto: {}", asunto);
            return;
        }

        if (destinatarios == null || destinatarios.isEmpty()) {
            log.warn("[EmailService] Sin destinatarios configurados. Asunto: {}", asunto);
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatarios.toArray(String[]::new));
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            mailSender.send(mensaje);
            log.info("[EmailService] Email enviado a {} destinatarios. Asunto: '{}'", destinatarios.size(), asunto);

        } catch (Exception e) {
            log.error("[EmailService] Error al enviar email '{}': {}", asunto, e.getMessage(), e);
        }
    }

    /**
     * Sobrecarga: envía a una cadena de destinatarios separados por coma.
     *
     * @param destinatariosCsv cadena CSV, e.g. "lt@empresa.com,sm@empresa.com"
     * @param asunto           asunto del correo
     * @param cuerpoHtml       contenido HTML
     */
    public void enviarHtml(String destinatariosCsv, String asunto, String cuerpoHtml) {
        if (destinatariosCsv == null || destinatariosCsv.isBlank()) {
            log.warn("[EmailService] Destinatarios vacíos. Asunto: {}", asunto);
            return;
        }
        List<String> lista = Arrays.stream(destinatariosCsv.split(","))
                .map(String::trim)
                .filter(d -> !d.isBlank())
                .toList();
        enviarHtml(lista, asunto, cuerpoHtml);
    }
}
