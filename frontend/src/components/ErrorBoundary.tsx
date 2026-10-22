import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Lỗi giao diện bắt được bởi ErrorBoundary:', error, errorInfo);
  }

  private handleGoHome = () => {
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-stone-100 text-stone-900 p-4 font-mono flex items-center justify-center">
          <div className="max-w-[540px] w-full border-4 border-stone-800 bg-[#e7dfd5] p-6 text-stone-900">
            <h2 className="text-xl font-black uppercase text-red-700 mb-2">Đã xảy ra lỗi hệ thống</h2>
            <p className="text-sm text-stone-700 mb-4">
              {this.state.error?.message || 'Giao diện gặp sự cố không mong muốn trong quá trình kết xuất.'}
            </p>
            <button
              type="button"
              onClick={this.handleGoHome}
              className="px-6 py-2 bg-stone-800 text-stone-100 font-bold border-2 border-stone-950 hover:bg-stone-700 cursor-pointer uppercase"
            >
              Về sảnh
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
