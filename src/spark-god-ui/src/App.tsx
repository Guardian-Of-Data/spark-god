import "./App.css"
import SparkGodApi from "./components/SparkGodApi"
import SparkGodChatbot from "./components/SparkGodChatbot"

function App() {
  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-b from-indigo-50 to-white">
      {/* Header */}
      <header className="w-full bg-white shadow-sm sticky top-0 z-50">
        {/* 메인과 동일한 폭 컨테이너 */}
        <div className="max-w-6xl mx-auto px-6 md:px-12 py-6">
          <div className="pl-8 ml-4 md:ml-6 lg:ml-8 py-6">
          <h1 className="text-5xl md:text-6xl font-extrabold text-slate-900 tracking-tight">
            SparkGod Dashboard
          </h1>
          </div>
        </div>
      </header>

      {/* Main */}
      <main className="max-w-6xl mx-auto px-6 md:px-12 py-10 w-full">
        <div className="bg-white shadow-lg rounded-2xl p-8 border border-gray-100">
          <SparkGodApi />
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-gray-50 border-t text-center py-6 text-sm text-gray-500">
        © {new Date().getFullYear()} SparkGod. Built with ❤️ for data engineers.
      </footer>

      {/* Chatbot (floating) */}
      <SparkGodChatbot />
    </div>
  )
}

export default App
