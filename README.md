# Secure-NotePad-Android-App
Overview:
This project is a Notepad Application built for Android that allows users to securely create, manage, and edit their personal notes. The application integrates user authentication, ensuring each user's notes are private and can only be accessed by that particular user. A robust SQLite database is employed for storing user credentials and notes, allowing seamless note creation, updates, and deletions.

The application now includes a comprehensive Admin Management System where an admin can manage registered users without storing admin credentials in the database, thereby enhancing security. The admin can view user details, search users by email, and delete users and their associated notes with ease.

Key Features:
User Authentication:

Users can sign up with a unique username, email, and password.
Login functionality with support for password recovery using a backup token.
Image verification during login (upcoming feature with OpenCV integration).
Note Management:

Users can add, edit, and delete notes.
Notes are user-specific, and only the user who created the notes can view or edit them.
Each note includes a heading and content, stored securely in an SQLite database.
Admin Login & User Management:

Admin can log in with hardcoded credentials (not stored in the database) to manage users.
Admin has access to view all registered users with their email addresses.
Advanced word-by-word search functionality to search users by email incrementally (as you type).
Admin can long-press on a user to delete them and all their notes from the database.
Optimized Search Functionality:

Word-by-word incremental search allows dynamic and real-time filtering of user data based on partial input.
Technologies Used:
Kotlin: Primary programming language for the entire Android application.
SQLite: For local storage of user data and notes, ensuring offline accessibility.
Android Studio: Integrated development environment for the project.
OpenCV (Upcoming): To implement image-based user verification during login.
Material Design: For creating a visually appealing and user-friendly interface.
RecyclerView: For efficient management of lists like user accounts and notes.
FloatingActionButton: For quick note creation in the Notepad section.
Google Play Services (Planned): Future integration for improved location-based note tagging.
Future Enhancements:
Image Verification: Implement OpenCV for facial recognition, allowing login using user images for added security.
Cloud Sync: Integrate AWS for cloud storage of notes, allowing users to sync across multiple devices.
Note Sharing: Add functionality for users to share notes with others securely.
Notification System: Notify users of upcoming events or deadlines based on their notes.
Dark Mode: User-configurable dark mode for better accessibility.
Problem-Solving Approach:
During the development process, I encountered several technical challenges, such as handling database queries, managing RecyclerViews, and implementing incremental search efficiently. To overcome these issues, I leveraged a variety of online resources, including StackOverflow and articles from the Android developer community. Moreover, I used ChatGPT to explore potential solutions, validate approaches, and get quick insights into Kotlin's syntax and functionality.

The process involved continuous learning and debugging, especially in areas like database integrity and user session management. Gradually, by implementing small features like incremental search and solving errors step-by-step, the application evolved into a comprehensive and fully-fledged solution.

Conclusion:
This Secure Notepad Application represents my journey through mobile app development, learning best practices, and addressing real-world challenges. With features like user-specific notes, admin management, and future cloud integration, the app is on track to become a powerful tool for personal organization and secure note-keeping.
