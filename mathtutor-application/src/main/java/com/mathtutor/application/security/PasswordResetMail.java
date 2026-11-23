package com.mathtutor.application.security;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** @brief Desacopla el correo SMTP de la respuesta HTTP y limita el trabajo en cola. */
@Component
public class PasswordResetMail {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetMail.class);
    private final PasswordResetService resets;
    private final MailSender sender;
    private final String from;
    private final URI page;
    private final ThreadPoolExecutor queue = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32));

    public PasswordResetMail(PasswordResetService resets, ObjectProvider<JavaMailSender> sender,
                             @Value("${app.password-reset.from:}") String from,
                             @Value("${app.password-reset.page:}") String page) {
        this.resets = resets;
        this.sender = sender.getIfAvailable();
        this.from = from;
        this.page = page.isBlank() ? null : trustedPage(page);
    }

    /**
     * @param email Dirección normalizada de la solicitud.
     * @return Si la solicitud se encoló, con independencia de la existencia de la cuenta.
     */
    public boolean enqueue(String email) {
        if (sender == null || from.isBlank() || page == null) return false;
        try {
            queue.execute(() -> deliver(email));
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    private void deliver(String email) {
        try {
            resets.issue(email).ifPresent(token -> {
                var message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(email);
                message.setSubject("Restablece tu contraseña de MathTutor");
                message.setText("Abre este enlace en los próximos 30 minutos:\n"
                        + page.toASCIIString() + "#/restablecer/" + token
                        + "\n\nSi no solicitaste el cambio, ignora este correo.");
                sender.send(message);
            });
        } catch (RuntimeException e) {
            // Los errores SMTP pueden incorporar el destinatario o el contenido del correo.
            LOG.warn("No se pudo procesar un correo de recuperación de contraseña");
        }
    }

    private static URI trustedPage(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getFragment() != null || uri.getQuery() != null) {
            throw new IllegalArgumentException("La página de recuperación debe ser una URL HTTPS sin consulta ni fragmento");
        }
        return uri;
    }

    /** @brief Detiene el trabajador SMTP al cerrar la aplicación. */
    @PreDestroy
    void close() {
        queue.shutdownNow();
    }
}
