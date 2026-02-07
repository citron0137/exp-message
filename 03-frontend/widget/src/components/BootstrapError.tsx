type BootstrapErrorProps = {
    message: string;
};

export default function BootstrapError({ message }: BootstrapErrorProps) {
    return (
        <div className="flex-1 p-4 bg-white">
            <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                {message}
            </div>
        </div>
    );
}
