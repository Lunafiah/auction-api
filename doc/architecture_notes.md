# Architecture Notes: Express.js vs Spring Security

Since you're coming from Node.js and Express, here is the mental model mapping for the JWT authentication structure we're building in Step 2:

## 1. The "Middleware" = The Filter Chain
In Express, you typically write a function like `verifyToken(req, res, next)` and slot it right into a route: 
`app.get('/auctions', verifyToken, controller.getAuctions)`

In Spring Boot, we don't attach security directly to individual controller routes. Instead, HTTP requests enter a **Security Filter Chain**. We build a globally aware `SecurityConfig` class that maps out entire paths in one place:
- "Allow all requests to `/api/auth/**`"
- "Require authentication for everything else."

We create a `JwtAuthenticationFilter` (which extends `OncePerRequestFilter`). This filter sits in the pipeline *before* the request even reaches your Controller. It checks the `Authorization` header, validates the JWT, and moves the request forward—exactly like calling `next()` in Express!

## 2. Setting Context = `SecurityContextHolder`
In Express, you normally attach the decoded user to the request: `req.user = decodedToken`.
In Spring, we use the `SecurityContextHolder`. This is much more robust because anywhere in your entire app (even deep inside a Service or Repository where you don't have access to the `req` object), you can instantly look up the currently logged-in user context.

## 3. JWT Utility = `jsonwebtoken` NPM Package
In Node, you used `jsonwebtoken.sign()` and `jsonwebtoken.verify()`.
In Java, we are adding the `jjwt` library to do literally the exact same thing. We centralize all the token creation and validation mechanics into a single `JwtUtil` component class.
