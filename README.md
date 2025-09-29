# EnsureBack

## Stripe Connect Login Configuration

EnsureBack now authenticates merchants via Stripe Connect rather than email/password credentials. Configure the following environment variables before running the application:

- `STRIPE_CONNECT_CLIENT_ID` – The Connect client identifier from your Stripe dashboard. It is required to build the OAuth authorization URL and to exchange the returned authorization code.
- `STRIPE_CONNECT_REDIRECT_URI` – The absolute URL Stripe should redirect to after the merchant approves access. This should resolve to the EnsureBack login route in your deployed environment (for local development the backend defaults to `http://localhost:5173/login`).
- `STRIPE_SECRET_KEY` – The platform secret key that is used to exchange the authorization code for the connected account identifier.

Ensure that the redirect URI configured in Stripe exactly matches the value supplied via `STRIPE_CONNECT_REDIRECT_URI`. The frontend login page expects the callback query parameters (`code`, `state`, `error`, and `error_description`) on that route and finalizes authentication by invoking the backend callback endpoint.
