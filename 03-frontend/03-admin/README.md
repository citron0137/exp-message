# Admin Frontend

This app is intentionally independent from `01-widget`.

Rules:
- Do not import files from `../01-widget`.
- Build admin-specific features inside this app.
- Shared extraction can be discussed later, not by default.

Core backend endpoints:
- Auth uses `/admin/auth/*`.
- Channel operations use `/admin/channels`.
- Widget integration and membership operations are nested under `/admin/channels/{channelId}`.
- Inbox operations are nested under `/admin/channels/{channelId}/conversations`.
