import { MediaCard } from './MediaCard.jsx';

export function MediaGrid({ media }) {
  if (!media.length) {
    return <p className="empty-state">No media yet. Extract a URL to begin.</p>;
  }

  return (
    <section className="media-grid">
      {media.map((item) => (
        <MediaCard key={`${item.type}-${item.url}`} item={item} />
      ))}
    </section>
  );
}
