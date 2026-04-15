export function MediaCard({ item }) {
  const isVideo = item.type === 'video';

  return (
    <article className="media-card">
      <div className="media-preview">
        {isVideo ? (
          <video src={item.url} controls preload="metadata" />
        ) : (
          <img src={item.url} alt={`${item.type} preview`} loading="lazy" />
        )}
      </div>
      <div className="media-card-footer">
        <span className="media-type">{item.type.toUpperCase()}</span>
        <a href={item.url} target="_blank" rel="noreferrer" download>
          Download
        </a>
      </div>
    </article>
  );
}
