interface FloatingButtonProps {
  onClick: () => void;
  isOpen: boolean;
}

export default function FloatingButton({ onClick, isOpen }: FloatingButtonProps) {
  return (
    <button
      onClick={onClick}
      className="fixed bottom-5 right-5 w-14 h-14 rounded-full bg-blue-600 hover:bg-blue-700 text-white shadow-lg hover:shadow-xl transition-all duration-200 flex items-center justify-center text-2xl z-[9999]"
      aria-label={isOpen ? 'Close chat' : 'Open chat'}
    >
      {isOpen ? '✕' : '💬'}
    </button>
  );
}
