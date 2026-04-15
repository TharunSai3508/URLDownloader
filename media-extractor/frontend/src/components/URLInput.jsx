export function URLInput({ value, onChange, onExtract, loading }) {
  return (
    <form
      className="url-form"
      onSubmit={(event) => {
        event.preventDefault();
        onExtract();
      }}
    >
      <input
        type="url"
        placeholder="Paste a media page URL (Instagram, YouTube, website...)"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required
      />
      <button type="submit" disabled={loading}>
        {loading ? 'Extracting...' : 'Extract'}
      </button>
    </form>
  );
}
