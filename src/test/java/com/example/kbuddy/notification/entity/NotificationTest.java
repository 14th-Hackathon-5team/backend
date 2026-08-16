package com.example.kbuddy.notification.entity;

import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    private User createUser() {
        return new User(
                AuthProvider.KAKAO,
                "kakao-1234",
                "student@example.com",
                "홍길동",
                "CN",
                2000,
                UserStatus.UNDERGRADUATE,
                "서울대학교",
                LocalDate.of(2022, 3, 1),
                VisaType.D2,
                true,
                LocalDate.of(2026, 9, 30),
                HousingType.DORMITORY,
                false,
                PartTimeStatus.SEARCHING,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
    }

    private Notification createNotification(User user) {
        return new Notification(
                user,
                NotificationCategory.VISA,
                "체류기간 만료 30일 전입니다",
                "현재 비자가 D-2입니다.",
                "체류기간 만료 전에 연장 절차를 준비하세요.",
                Map.of("visaType", "D2", "daysLeft", 30),
                Map.of("dataset", "law", "sourceName", "국가법령정보센터"),
                5,
                NotificationTriggerType.VISA_EXPIRATION,
                LocalDate.of(2026, 8, 31)
        );
    }

    private Notification createNotificationWithPriority(int priority) {
        return new Notification(
                createUser(),
                NotificationCategory.VISA,
                "제목",
                "이유",
                "요약",
                Map.of("key", "value"),
                null,
                priority,
                NotificationTriggerType.VISA_EXPIRATION,
                LocalDate.of(2026, 8, 31)
        );
    }

    @Test
    void priority가_1부터_5까지면_정상적으로_생성된다() {
        for (int priority = 1; priority <= 5; priority++) {
            int finalPriority = priority;
            assertThatCode(() -> createNotificationWithPriority(finalPriority))
                    .as("priority=%d는 정상 생성되어야 한다", priority)
                    .doesNotThrowAnyException();
            assertThat(createNotificationWithPriority(priority).getPriority()).isEqualTo(priority);
        }
    }

    @Test
    void priority가_1보다_작거나_5보다_크면_IllegalArgumentException이_발생한다() {
        assertThatThrownBy(() -> createNotificationWithPriority(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createNotificationWithPriority(6))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 생성자로_Notification을_생성하면_전달한_값이_각_필드에_정상적으로_저장된다() {
        User user = createUser();
        Notification notification = createNotification(user);

        assertThat(notification.getUser()).isEqualTo(user);
        assertThat(notification.getCategory()).isEqualTo(NotificationCategory.VISA);
        assertThat(notification.getTitle()).isEqualTo("체류기간 만료 30일 전입니다");
        assertThat(notification.getReason()).isEqualTo("현재 비자가 D-2입니다.");
        assertThat(notification.getSummary()).isEqualTo("체류기간 만료 전에 연장 절차를 준비하세요.");
        assertThat(notification.getDetails()).containsEntry("visaType", "D2").containsEntry("daysLeft", 30);
        assertThat(notification.getSource()).containsEntry("dataset", "law");
        assertThat(notification.getPriority()).isEqualTo(5);
        assertThat(notification.getTriggerType()).isEqualTo(NotificationTriggerType.VISA_EXPIRATION);
        assertThat(notification.getTriggerDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void 생성_직후_isRead는_false이고_readAt은_null이다() {
        Notification notification = createNotification(createUser());

        assertThat(notification.getIsRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();
    }

    @Test
    void source가_null이어도_예외_없이_생성된다() {
        Notification notification = new Notification(
                createUser(),
                NotificationCategory.LEGAL,
                "제목",
                "이유",
                "요약",
                Map.of("key", "value"),
                null,
                3,
                NotificationTriggerType.ALIEN_REGISTRATION,
                LocalDate.of(2026, 9, 1)
        );

        assertThat(notification.getSource()).isNull();
    }

    @Test
    void markAsRead를_호출하면_isRead가_true가_되고_readAt이_설정된다() {
        Notification notification = createNotification(createUser());

        notification.markAsRead();

        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void 이미_읽은_Notification을_다시_읽음_처리해도_예외가_발생하지_않고_readAt이_유지된다() {
        Notification notification = createNotification(createUser());
        notification.markAsRead();
        var firstReadAt = notification.getReadAt();

        notification.markAsRead();

        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    void 기본_생성자로_생성하면_필드가_모두_초기값인_상태로_생성된다() {
        Notification notification = new Notification();

        assertThat(notification.getId()).isNull();
        assertThat(notification.getUser()).isNull();
        assertThat(notification.getIsRead()).isNull();
        assertThat(notification.getCreatedAt()).isNull();
    }

    @Test
    void Notification_클래스는_Entity이며_테이블명은_notifications이다() {
        Entity entityAnnotation = Notification.class.getAnnotation(Entity.class);
        Table tableAnnotation = Notification.class.getAnnotation(Table.class);

        assertThat(entityAnnotation).isNotNull();
        assertThat(tableAnnotation).isNotNull();
        assertThat(tableAnnotation.name()).isEqualTo("notifications");
    }

    @Test
    void id_필드는_IDENTITY_전략의_기본키이며_컬럼명은_notification_id다() throws NoSuchFieldException {
        Field idField = Notification.class.getDeclaredField("id");

        assertThat(idField.getAnnotation(Id.class)).isNotNull();
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
        assertThat(idField.getAnnotation(Column.class).name()).isEqualTo("notification_id");
    }

    @Test
    void category와_triggerType_필드는_EnumType_STRING으로_매핑된다() throws NoSuchFieldException {
        String[] enumFieldNames = {"category", "triggerType"};

        for (String fieldName : enumFieldNames) {
            Field field = Notification.class.getDeclaredField(fieldName);
            Enumerated enumerated = field.getAnnotation(Enumerated.class);

            assertThat(enumerated)
                    .as("%s 필드에는 @Enumerated가 존재해야 한다", fieldName)
                    .isNotNull();
            assertThat(enumerated.value())
                    .as("%s 필드는 EnumType.STRING이어야 한다", fieldName)
                    .isEqualTo(EnumType.STRING);
        }
    }

    @Test
    void details와_source_필드는_MySQL_JSON_컬럼으로_매핑된다() throws NoSuchFieldException {
        Field detailsField = Notification.class.getDeclaredField("details");
        Field sourceField = Notification.class.getDeclaredField("source");

        assertThat(detailsField.getAnnotation(JdbcTypeCode.class)).isNotNull();
        assertThat(detailsField.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.JSON);
        assertThat(detailsField.getAnnotation(Column.class).columnDefinition()).isEqualTo("json");
        assertThat(detailsField.getAnnotation(Column.class).nullable()).isFalse();

        assertThat(sourceField.getAnnotation(JdbcTypeCode.class)).isNotNull();
        assertThat(sourceField.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.JSON);
        assertThat(sourceField.getAnnotation(Column.class).columnDefinition()).isEqualTo("json");
        assertThat(sourceField.getAnnotation(Column.class).nullable()).isTrue();
    }

    @Test
    void user_필드는_notNull_FK로_매핑된다() throws NoSuchFieldException {
        Field userField = Notification.class.getDeclaredField("user");

        assertThat(userField.getAnnotation(jakarta.persistence.ManyToOne.class)).isNotNull();
        assertThat(userField.getAnnotation(jakarta.persistence.JoinColumn.class).name()).isEqualTo("user_id");
        assertThat(userField.getAnnotation(jakarta.persistence.JoinColumn.class).nullable()).isFalse();
    }

    @Test
    void readAt_필드는_nullable_컬럼으로_매핑된다() throws NoSuchFieldException {
        Column column = Notification.class.getDeclaredField("readAt").getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo("read_at");
        assertThat(column.nullable()).isTrue();
    }
}
