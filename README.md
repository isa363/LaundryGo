# LaundryGo

LaundryGo is a smart laundry management mobile application built to modernize how shared laundry systems work in residential buildings.

It combines a mobile app, hardware integration, and real-time communication to create a seamless experience for both users and administrators.

---

## What it does

LaundryGo allows users to:

- Track laundry machine availability in real-time  
- Get notified when their laundry is done  
- Report issues through a ticket system  
- Chat directly with administrators  

Admins can:

- Manage users and access permissions  
- Monitor machine usage  
- Respond to tickets in real-time  
- Close, archive, and delete tickets  
- Control building-specific settings  

---

## Tech Stack

- **Android Studio (Java)** — main mobile application  
- **Firebase Authentication** — user login & security  
- **Firebase Firestore** — real-time database  
- **Arduino + Sensors** — detect machine activity (running / finished)  
- **Material UI Components** — clean and responsive interface  

---

## How it works (high-level)

Laundry machines are connected to sensors (via Arduino) that detect activity (vibration / usage).  
This data is sent to the app backend and reflected in real-time inside the mobile app.

Users can see:
- which machines are busy  
- which are available  
- when their cycle is finished  

The system also includes a built-in ticket/chat feature for reporting issues and communicating with admins.

---

## Goal

The goal of LaundryGo is to eliminate:
- guessing if machines are free  
- waiting around for laundry  
- poor communication in shared buildings  

…and replace it with a **smart, connected laundry experience**.

---

## Contributors

- Stephany Carvajal 40289882
- Lucy Dai 40275226
- Isabella Barange Garzon 40283184
- Anais Kiam Tchos 40171144
- Nkrumah Leugoue Nougoue 40258711

---
