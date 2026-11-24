# 🎉 FestiveHub

**FestiveHub** is a native Android event management and campus festival platform built with Java and Firebase. It streamlines event discovery, student registrations, volunteer coordination, and QR-based attendee check-ins across three role-based portals: **Students**, **Volunteers**, and **Admins**[cite: 1].

---

## ✨ Key Features

### 🎓 Student Portal
* **Authentication & Profile:** Secure email/password sign-up, login, password recovery, and personal profile management (`Login`, `SignUp`, `ForgotPassword`, `Profile`)[cite: 1].
* **Event Discovery:** Featured event carousel banner and categorized event listings (`CULTURAL FEST`, `MARKETPLACE`, `MUSIC`) on the home feed (`Home`, `BannerAdapter`, `EventAdapter`)[cite: 1].
* **Event Registration & Digital Tickets:** View event descriptions, pricing (`Free Entry` or paid in ₹), and register for events (`Event_Detail`, `RegisteredEventAdapter`)[cite: 1].

### 🤝 Volunteer Portal
* **Role-Based Dashboard:** Dedicated volunteer home screen showing total platform events, assigned event counts, and upcoming schedules (`VolunteerHomeScreen`)[cite: 1].
* **Assigned Events & Schedules:** View events assigned by administrators along with live registration counts and time-slotted event schedules (`VolunteerAssignedEvents`, `VolunteerEventDetails`)[cite: 1].
* **QR Code Check-In Scanner:** Built-in camera QR scanner to validate attendee registrations and mark check-ins in real time (`Volunteers_QR`)[cite: 1].

### 🛠️ Admin Dashboard
* **Centralized Management:** Dynamic CRUD interface (`AdminDashboard`, `ManageList`, `ManageForm`) to add, edit, and manage[cite: 1]:
  * **Events** (title, category, date & location, price, banner image, description, schedule, featured status)[cite: 1]
  * **Volunteers** (profile details and event assignments)[cite: 1]
  * **Students** (student directory and college mapping)[cite: 1]
  * **Colleges** & **Notifications**[cite: 1]
* **Registration Tracking:** View registered attendees per event (`RegisteredUsers`)[cite: 1].

---

## 🏗️ Tech Stack & Architecture

* **Language:** Java (Android SDK)[cite: 1]
* **UI Components:** XML Layouts, RecyclerView, ConstraintLayout, ViewPager2 / Banner Carousel[cite: 1]
* **Backend & Database:**
  * **Firebase Authentication** (Email/Password login & role routing)[cite: 1]
  * **Cloud Firestore** (NoSQL real-time database with role-based security rules)[cite: 1]
* **Libraries:**
  * Firebase BOM / Auth / Firestore (`google-services.json`)[cite: 1]
  * ZXing Android Embedded (QR code scanning)[cite: 1]

---

## 📂 Project Structure

```text
app/src/main/java/com/example/festivehub/
├── MainActivity.java              # Splash / Entry point[cite: 1]
├── Login.java                     # Role-based authentication (Student / Volunteer / Admin)[cite: 1]
├── SignUp.java                    # New user registration[cite: 1]
├── ForgotPassword.java            # Password reset flow[cite: 1]
├── Home.java                      # Student dashboard & featured events[cite: 1]
├── Event_Detail.java              # Event info & student registration[cite: 1]
├── Profile.java                   # User profile & registered events[cite: 1]
├── VolunteerHomeScreen.java       # Volunteer overview dashboard[cite: 1]
├── VolunteerAssignedEvents.java   # List of events assigned to logged-in volunteer[cite: 1]
├── VolunteerEventDetails.java     # Event details, slot schedule & QR scanner trigger[cite: 1]
├── Volunteers_QR.java             # Camera QR scanner for attendee check-in[cite: 1]
├── AdminDashboard.java            # Admin control panel[cite: 1]
├── ManageList.java                # Generic list view for Admin entities[cite: 1]
├── ManageForm.java                # Dynamic add/edit form for Admin entities[cite: 1]
├── AdminEntity.java               # Schema definitions for Events, Volunteers, Students, etc.[cite: 1]
└── RegisteredUsers.java           # View users registered for events[cite: 1]
