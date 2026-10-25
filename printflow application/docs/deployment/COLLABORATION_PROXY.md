# Collaboration WebSocket routing

The Next.js rewrite in `frontend/next.config.mjs` proxies HTTP API requests only. Do not assume it forwards WebSocket Upgrade requests.

## Native local development

- Run the browser app on port 3000 and Spring on port 8080.
- The collaboration client uses `ws://localhost:8080/collaboration/ws` (or `127.0.0.1` when that host is used) unless `NEXT_PUBLIC_COLLABORATION_URL` is set.
- Set `APP_COLLABORATION_ALLOWED_ORIGINS` to the exact frontend origin, for example `http://localhost:3000`. For both local host spellings, explicitly list both origins separated by commas.
- The session cookie is same-site across these localhost ports and stays HttpOnly. STOMP CONNECT carries the CSRF token returned by `/api/v1/auth/csrf`.

## Production reverse proxy

Build the frontend with `NEXT_PUBLIC_COLLABORATION_URL=wss://<public-site>/collaboration/ws`. Set Spring `APP_COLLABORATION_ALLOWED_ORIGINS=https://<public-site>` and keep secure session cookies enabled. The public TLS reverse proxy must preserve the browser's `Origin` header and WebSocket Upgrade headers while routing this exact path to the API service. Do not expose the API directly to browsers or put credentials in the URL.

Example Nginx configuration. The `map` and upstream belong in the `http` context; place the location inside the existing TLS `server` block:

```nginx
http {
    map $http_upgrade $connection_upgrade {
        default upgrade;
        ''      close;
    }

    upstream printflow_api {
        server 127.0.0.1:8080;
    }

    server {
        # TLS configuration for the application host is managed here.
        location = /collaboration/ws {
            proxy_pass http://printflow_api/collaboration/ws;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
            proxy_set_header Host $host;
            proxy_set_header Origin $http_origin;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_read_timeout 70s;
            proxy_buffering off;
        }
    }
}
```

Keep the in-memory broker on one Spring instance. Multiple API replicas need a shared broker/event relay before collaboration traffic is load-balanced across them. Existing REST workflows continue to work if the WebSocket service is unavailable.
