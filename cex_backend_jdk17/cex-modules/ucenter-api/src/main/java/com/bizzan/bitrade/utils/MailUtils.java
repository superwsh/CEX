package com.bizzan.bitrade.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

public class MailUtils {

//    // 渲染FreeMarker模板
//    public static String renderTemplate(String templateName, Map<String, Object> model) throws Exception {
//        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
//        cfg.setClassForTemplateLoading(MailUtils.class, "/templates");
//        Template template = cfg.getTemplate(templateName, "UTF-8");
//        return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
//    }
//
//    // 发送HTML邮件
//    public static void sendMail(String to, String subject, String content, boolean isHtml) throws MessagingException {
//        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
//        helper.setFrom(from); // 从配置文件读取默认发件人
//        helper.setTo(to.split(","));
//        helper.setSubject(subject);
//        helper.setText(content, isHtml);
//        javaMailSender.send(mimeMessage);
//    }

}
