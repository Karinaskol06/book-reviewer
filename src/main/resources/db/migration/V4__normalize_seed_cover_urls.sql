-- Point seeded local upload cover keys at classpath-static images
-- so clones work without needing the uploads-book-reviewer/ folder.

UPDATE public.books
SET cover_url = '/images/book-covers/the-hunger-games.jpg'
WHERE id = 16
  AND (cover_url IS NULL OR cover_url LIKE 'covers/%' OR cover_url LIKE '/uploads-book-reviewer/covers/%');

UPDATE public.books
SET cover_url = '/images/book-covers/1984.jpg'
WHERE id = 17
  AND (cover_url IS NULL OR cover_url LIKE 'covers/%' OR cover_url LIKE '/uploads-book-reviewer/covers/%');

UPDATE public.books
SET cover_url = '/images/book-covers/brave-new-world.jpg'
WHERE id = 18
  AND (cover_url IS NULL OR cover_url LIKE 'covers/%' OR cover_url LIKE '/uploads-book-reviewer/covers/%');

UPDATE public.books
SET cover_url = '/images/book-covers/if-we-were-villains.jpg'
WHERE id = 19
  AND (cover_url IS NULL OR cover_url LIKE 'covers/%' OR cover_url LIKE '/uploads-book-reviewer/covers/%');

-- Same for seeded avatars on DBs that already had users before V3.
UPDATE public.users
SET avatar_url = '/images/avatars/karina.jpg'
WHERE id = 2
  AND avatar_url IN (
      'avatars/72c333b8-dbc1-4952-8dc3-ed09f62d9f65.jpg',
      '/uploads-book-reviewer/avatars/72c333b8-dbc1-4952-8dc3-ed09f62d9f65.jpg'
  );

UPDATE public.users
SET avatar_url = '/images/avatars/artem.jpg'
WHERE id = 3
  AND avatar_url IN (
      'avatars/2e6fee6b-6b25-4cbd-8218-133159a66b9c.jpg',
      '/uploads-book-reviewer/avatars/2e6fee6b-6b25-4cbd-8218-133159a66b9c.jpg'
  );

UPDATE public.users
SET avatar_url = '/images/avatars/berkobobus.jpg'
WHERE id = 6
  AND avatar_url IN (
      'avatars/c0e8ad25-ae6d-4f1a-9ad2-441a0ec279e7.jpg',
      '/uploads-book-reviewer/avatars/c0e8ad25-ae6d-4f1a-9ad2-441a0ec279e7.jpg'
  );
