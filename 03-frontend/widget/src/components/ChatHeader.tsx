

export default function ChatHeader() {
    return (
        <div className="bg-blue-600 text-white p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-blue-500 flex items-center justify-center text-lg">
                    💬
                </div>
                <div>
                    <h3 className="font-semibold">Chat room name</h3>
                    <p className="text-xs text-blue-100">Chat room Description</p>
                </div>
            </div>

        </div>
    );
}
