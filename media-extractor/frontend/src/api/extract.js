const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:4000';

export const extractMedia = async (url) => {
  const response = await fetch(`${API_BASE_URL}/extract`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ url })
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.error || 'Extraction failed');
  }

  return data;
};
