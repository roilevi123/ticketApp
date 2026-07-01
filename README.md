# TicketApp - Initial State & Configuration

README קצר שמסביר איך להגדיר את החיבור למסד הנתונים, איך להפעיל את מנגנון ה־Initial State, ואיך לכתוב את הקובץ `initial-state.txt`.

---

## 1. קובץ הקונפיגורציה

הקובץ המרכזי הוא:

```text
src/main/resources/application.properties
```

דוגמה לקונפיגורציה עבור PostgreSQL:

```properties
spring.datasource.url=jdbc:postgresql://136.115.146.17:5432/ticketapp_db?connectTimeout=5
spring.datasource.username=ticketapp_user
spring.datasource.password=BGUticketapp1!
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.keepalive-time=60000
spring.datasource.hikari.validation-timeout=3000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.maximum-pool-size=15
spring.datasource.hikari.connection-timeout=15000
spring.datasource.hikari.leak-detection-threshold=30000

spring.jpa.properties.hibernate.jdbc.batch_size=100
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

repository.type=DB
spring.task.scheduling.enabled=false
```

> חשוב: בקובץ אמיתי לא מומלץ לשמור סיסמה גלויה בתוך Git. עדיף להשתמש במשתני סביבה.

---

## 2. הגדרות Initial State

ה־Initial State מאפשר לטעון מידע התחלתי למערכת בזמן עליית השרת.

ההגדרות נמצאות גם הן ב־`application.properties`:

```properties
initial.state.file=initial-state.txt
initial.state.skipExisting=true
initial.state.reset=true
initial.state.enabled=true
```

### הסבר על כל שדה

| שדה | משמעות |
|---|---|
| `initial.state.file` | שם הקובץ שממנו נטענות הפקודות. ברירת מחדל: `initial-state.txt` |
| `initial.state.skipExisting` | אם הערך `true`, המערכת תדלג על שורות שיוצרות מידע שכבר קיים |
| `initial.state.reset` | אם הערך `true`, המערכת תמחק מידע קיים לפני טעינת הקובץ |
| `initial.state.enabled` | מפעיל או מכבה את מנגנון הטעינה |

---

## 3. איפה לשים את initial-state.txt

יש לשים את הקובץ כאן:

```text
src/main/resources/initial-state.txt
```

המערכת מחפשת את הקובץ מתוך ה־classpath, לכן הוא חייב להיות בתוך `resources`.

---

## 4. איך הקובץ initial-state.txt עובד

כל שורה בקובץ היא פעולה אחת.

הפורמט הכללי:

```text
action arg1 arg2 arg3 ...
```

המערכת קוראת את הקובץ שורה־שורה ומריצה כל פעולה לפי הסדר.

שורות ריקות או שורות שמתחילות ב־`#` לא ירוצו.

דוגמה:

```text
# users
registerAdmin admin Admin123 30 admin@example.com
register user1 Pass123 22 user1@example.com
login admin Admin123

# company
create-company admin BGU_Events

# event
create-event admin BGU_Events Rock_Night Coldplay MUSIC 150 Beer_Sheva 30 10 20
```

---

## 5. שימוש ברווחים בטקסט

הקובץ מפריד בין ערכים לפי רווחים.

לכן, אם רוצים שם עם כמה מילים, משתמשים בקו תחתון `_`.

לדוגמה:

```text
BGU_Events
Rock_Night
Beer_Sheva
```

בפועל המערכת תהפוך את זה ל:

```text
BGU Events
Rock Night
Beer Sheva
```

---

## 6. סדר הפעולות חשוב

צריך לכתוב את הפעולות לפי סדר הגיוני.

לדוגמה:

1. קודם יוצרים משתמשים
2. אחר כך עושים login
3. אחר כך יוצרים חברה
4. אחר כך ממנים מנהלים או בעלים
5. אחר כך יוצרים אירועים
6. אחר כך מוסיפים lottery / discounts / policies

אם פעולה צריכה משתמש מחובר, חייבת להיות לפני כן שורת `login`.

---

## 7. פעולות נתמכות

### Register

```text
register username password age email
```

דוגמה:

```text
register user1 Pass123 22 user1@example.com
```

---

### Login

```text
login username password
```

דוגמה:

```text
login user1 Pass123
```

---

### Register Admin

```text
registerAdmin username password age email
```

דוגמה:

```text
registerAdmin admin Admin123 30 admin@example.com
```

---

### Create Company

```text
create-company username companyName
```

דוגמה:

```text
create-company admin BGU_Events
```

---

### Appoint Manager

```text
appoint-manager appointerUsername managerUsername companyName permissions
```

דוגמה:

```text
appoint-manager admin manager1 BGU_Events CREATE_EVENT,EDIT_EVENT
```

---

### Approve Manager

```text
approve-manager username companyName
```

דוגמה:

```text
approve-manager manager1 BGU_Events
```

---

### Appoint Owner

```text
appoint-owner appointerUsername ownerUsername companyName
```

דוגמה:

```text
appoint-owner admin owner1 BGU_Events
```

---

### Approve Owner

```text
approve-owner username companyName
```

דוגמה:

```text
approve-owner owner1 BGU_Events
```

---

### Create Event

```text
create-event username companyName eventName artistName type price location daysFromNow rows cols
```

דוגמה:

```text
create-event admin BGU_Events Rock_Night Coldplay MUSIC 150 Beer_Sheva 30 10 20
```

המשמעות:

| ערך | הסבר |
|---|---|
| `daysFromNow` | בעוד כמה ימים האירוע יתקיים |
| `rows` | מספר שורות במפה |
| `cols` | מספר עמודות במפה |

כרגע המפה נוצרת אוטומטית כולה מסוג `SEAT`.

---

### Configure Lottery

```text
configure-lottery username companyName eventName minutesFromNow maxWinners
```

דוגמה:

```text
configure-lottery admin BGU_Events Rock_Night 60 5
```

---

## 8. Discounts

### Simple Discount

```text
discount-simple username targetId targetType percentage companyName saveAs
```

דוגמה:

```text
discount-simple admin Rock_Night EVENT 10 BGU_Events d1
```

---

### Quantity Discount

```text
discount-quantity username targetId targetType percentage minQuantity companyName saveAs
```

דוגמה:

```text
discount-quantity admin Rock_Night EVENT 15 3 BGU_Events d2
```

---

### Coupon Discount

```text
discount-coupon username targetId targetType code percentage companyName saveAs
```

דוגמה:

```text
discount-coupon admin Rock_Night EVENT SAVE10 10 BGU_Events d3
```

---

### Sum Discount

```text
discount-sum username targetId targetType companyName saveAs id1,id2,id3
```

דוגמה:

```text
discount-sum admin Rock_Night EVENT BGU_Events dSum d1,d2,d3
```

---

### Max Discount

```text
discount-max username targetId targetType companyName saveAs id1,id2,id3
```

דוגמה:

```text
discount-max admin Rock_Night EVENT BGU_Events dMax d1,d2,d3
```

---

## 9. Purchase Policies

### Age Policy

```text
policy-age username targetId targetType minAge saveAs
```

דוגמה:

```text
policy-age admin Rock_Night EVENT 18 p1
```

---

### Quantity Policy

```text
policy-quantity username targetId targetType minQuantity maxQuantity saveAs
```

דוגמה:

```text
policy-quantity admin Rock_Night EVENT 1 4 p2
```

---

### AND Policy

```text
policy-and username targetId targetType saveAs id1,id2
```

דוגמה:

```text
policy-and admin Rock_Night EVENT pAnd p1,p2
```

---

### OR Policy

```text
policy-or username targetId targetType saveAs id1,id2
```

דוגמה:

```text
policy-or admin Rock_Night EVENT pOr p1,p2
```

---

## 10. דוגמה מלאה לקובץ initial-state.txt

```text
# admins
registerAdmin admin Admin123 30 admin@example.com
login admin Admin123

# users
register manager1 Pass123 25 manager1@example.com
register owner1 Pass123 27 owner1@example.com
login manager1 Pass123
login owner1 Pass123

# company
create-company admin BGU_Events

# appointments
appoint-manager admin manager1 BGU_Events CREATE_EVENT,EDIT_EVENT
approve-manager manager1 BGU_Events

appoint-owner admin owner1 BGU_Events
approve-owner owner1 BGU_Events

# events
create-event admin BGU_Events Rock_Night Coldplay MUSIC 150 Beer_Sheva 30 10 20
configure-lottery admin BGU_Events Rock_Night 60 5

# discounts
discount-simple admin Rock_Night EVENT 10 BGU_Events d1
discount-quantity admin Rock_Night EVENT 15 3 BGU_Events d2
discount-coupon admin Rock_Night EVENT SAVE10 10 BGU_Events d3
discount-sum admin Rock_Night EVENT BGU_Events dSum d1,d2,d3
discount-max admin Rock_Night EVENT BGU_Events dMax d1,d2,d3

# policies
policy-age admin Rock_Night EVENT 18 p1
policy-quantity admin Rock_Night EVENT 1 4 p2
policy-and admin Rock_Night EVENT pAnd p1,p2
policy-or admin Rock_Night EVENT pOr p1,p2
```

---

## 11. הרצה

כדי להריץ עם טעינת Initial State:

```bash
mvn spring-boot:run
```

או דרך IntelliJ:

1. לפתוח את הפרויקט
2. לוודא ש־`application.properties` מוגדר נכון
3. לוודא ש־`initial-state.txt` נמצא בתוך `src/main/resources`
4. להריץ את המחלקה הראשית של Spring Boot

---

## 12. כיבוי Initial State

אם לא רוצים שהמידע ייטען בכל הרצה:

```properties
initial.state.enabled=false
```

אם רוצים לטעון אבל לא למחוק מידע קיים:

```properties
initial.state.reset=false
```

אם רוצים שהמערכת לא תיפול על מידע שכבר קיים:

```properties
initial.state.skipExisting=true
```

---

## 13. הערות חשובות

- אם `initial.state.reset=true`, מידע קיים במסד הנתונים יימחק לפני הטעינה.
- אם יש פעולה שנכשלת, המערכת תעצור ותציג באיזו שורה הייתה הבעיה.
- אם משתמש לא עשה `login`, אי אפשר להשתמש בו בפעולות שדורשות token.
- שמות עם רווחים צריכים להיכתב עם `_`.
- הסדר בקובץ חשוב מאוד.
- במצב `test` ה־DataInitializer לא רץ בגלל `@Profile("!test")`.
