# Chat Widget

Embeddable chat widget for exp-message service. Channel Talk / Intercom style floating chat button.

## Features

- 🎨 Modern UI with Tailwind CSS
- 💬 Floating button with chat window
- 🔄 Real-time messaging with WebSocket
- 🎯 Shadow DOM for style isolation
- 📦 Single bundle output (IIFE)
- 🚀 Easy integration into any website

## Tech Stack

- **React 19** + TypeScript
- **Vite** - Build tool
- **Tailwind CSS** - Styling
- **Zustand** - State management
- **@stomp/stompjs** - WebSocket client
- **Axios** - HTTP client

## Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Usage

### Basic Integration

```html
<!DOCTYPE html>
<html>
<head>
  <title>Your Website</title>
</head>
<body>
  <h1>Your Content</h1>
  
  <!-- Chat Widget -->
  <script src="https://message.rahoon.site/widget.js"></script>
  <script>
    ChatWidget.init({
      apiUrl: 'https://message.rahoon.site/api',
      wsUrl: 'wss://message.rahoon.site/ws'
    });
  </script>
</body>
</html>
```

### Configuration Options

```typescript
ChatWidget.init({
  apiUrl: 'https://message.rahoon.site/api',  // Backend API URL
  wsUrl: 'wss://message.rahoon.site/ws',      // WebSocket URL
  theme: 'light'                               // Theme (light | dark)
});
```

## Project Structure

```
src/
├── components/          # UI components
│   ├── FloatingButton.tsx
│   ├── ChatWindow.tsx
│   ├── ChatHeader.tsx
│   ├── MessageList.tsx
│   ├── MessageBubble.tsx
│   └── MessageInput.tsx
├── lib/                # Utilities
│   ├── api/           # API client
│   └── websocket/     # WebSocket client
├── hooks/             # Custom React hooks
├── styles/            # Global styles
│   └── index.css
├── Widget.tsx         # Main widget component
└── main.tsx          # Entry point
```

## Build Output

After running `npm run build`, the widget will be available at:
- `dist/widget.iife.js` - Single bundled JavaScript file
- `dist/widget.css` - Styles (inlined in JS for easier integration)

## Deployment

Upload the built files to your CDN or static file server:

```bash
# Example: Copy to static server
cp dist/widget.iife.js /var/www/static/widget.js
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## License

MIT
