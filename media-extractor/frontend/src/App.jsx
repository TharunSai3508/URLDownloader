import { useMemo, useState } from 'react';
import { URLInput } from './components/URLInput.jsx';
import { MediaGrid } from './components/MediaGrid.jsx';
import { extractMedia } from './api/extract.js';

function App() {
  const [url, setUrl] = useState('');
  const [result, setResult] = useState({ images: [], videos: [], gifs: [], source: null });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const mediaItems = useMemo(
    () => [
      ...result.images.map((entry) => ({ type: 'image', url: entry })),
      ...result.videos.map((entry) => ({ type: 'video', url: entry })),
      ...result.gifs.map((entry) => ({ type: 'gif', url: entry }))
    ],
    [result]
  );

  const onExtract = async () => {
    if (!url.trim()) return;

    setLoading(true);
    setError('');

    try {
      const data = await extractMedia(url.trim());
      setResult(data);

      if (!data.images.length && !data.videos.length && !data.gifs.length) {
        setError('No media was found at this URL.');
      }
    } catch (err) {
      setResult({ images: [], videos: [], gifs: [], source: null });
      setError(err.message || 'Unable to extract media');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="container">
      <header>
        <h1>Media Extractor</h1>
        <p>Extract images, videos, and GIFs from public URLs.</p>
      </header>

      <URLInput value={url} onChange={setUrl} onExtract={onExtract} loading={loading} />

      {error && <p className="error">{error}</p>}
      {result.source && !error && <p className="source">Scraper used: {result.source}</p>}

      <MediaGrid media={mediaItems} />
    </main>
  );
}

export default App;
