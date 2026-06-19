import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { ThemeProvider } from '@/components/theme-provider'
import NovelList from './pages/NovelList'
import CreateNovel from './pages/CreateNovel'
import NovelEditor from './pages/NovelEditor'

function App() {
  return (
    <ThemeProvider defaultTheme="light" storageKey="zwriter-ui-theme">
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<NovelList />} />
          <Route path="/create" element={<CreateNovel />} />
          <Route path="/novel/:novelId/edit" element={<NovelEditor />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  )
}

export default App
