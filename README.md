# Setup
## Admin seed

Upon the system starting, the database will attempt to seed an admin account. In order to do so, its needs to find the admin seed file located in `data/admin.json`. The file should contain the following:

```
{
    "email" : "",
    "password" : ""
}
```

You fill out the email and password that will be used to seed the admin account

## Environment variables

In the root folder, you will need a `.env` file that will contain environment variables, such as API keys. The file should contain the following:

```
STRIPE_SECRET_KEY=""
GEMINI_API_KEY=""
EMAIL_USER=
EMAIL_APP_PASSWORD=
```

# Using the system
## Admin log in

If the admin account was seeded, you will be able to log into it at `/login`. Input the email and password from the admin seed file and then input the 2FA code that is sent to your email. Once logged in, you will be taken to `/admin`. Along the top you will have options to manage staff, manage flights, manage plane data, manage planes and manage routes.

## Staff

To use the system as a staff member, an admin has to first create a staff account for you. Once done, navigate to `/stafflogin` and enter the login details used to create the staff account. Once logged in, you will be able to see flights that have been assigned to you.

## User

As a user, you can naviage to `/book` and search for available flights. As the database will be quite empty, it will be hard to find any available flights. However, on the admin page, there will be a short list of flights that are currently in the database. You can use this list of flights to book as the user. Clicking on a flight then allows you to book seats for that flight. As the website is not live, you can use fake credit card information to pay for the flight (card number 4242 4242 4242 4242 and input any valid date and any number for the CVC and zip code.
