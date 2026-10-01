-- Flyway V1 baseline: schema captured from live Docker Postgres (Hibernate ddl-auto era).
-- Existing DBs are baselined at version 1 (this script is not re-executed).
-- Fresh empty DBs run this migration to create the full schema.
--
-- PostgreSQL database dump
--


-- Dumped from database version 16.13
-- Dumped by pg_dump version 16.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: activity_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.activity_events (
    id bigint NOT NULL,
    actor_id bigint NOT NULL,
    additional_data text,
    book_id bigint,
    created_at timestamp(6) without time zone,
    review_id bigint,
    target_user_id bigint NOT NULL,
    type character varying(255) NOT NULL
);


--
-- Name: activity_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.activity_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: activity_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.activity_events_id_seq OWNED BY public.activity_events.id;


--
-- Name: book_genres; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.book_genres (
    book_id bigint NOT NULL,
    genre character varying(255)
);


--
-- Name: books; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.books (
    id bigint NOT NULL,
    author character varying(255) NOT NULL,
    average_rating double precision,
    cover_url character varying(255),
    created_at timestamp(6) without time zone,
    description text,
    normalized_title character varying(255) NOT NULL,
    publication_year integer,
    rating_count integer,
    title character varying(255) NOT NULL,
    total_reviews integer,
    normalized_author character varying(255) NOT NULL,
    pacing character varying(255),
    CONSTRAINT books_pacing_check CHECK (((pacing)::text = ANY ((ARRAY['SLOW'::character varying, 'MEDIUM'::character varying, 'FAST'::character varying])::text[])))
);


--
-- Name: books_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.books_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: books_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.books_id_seq OWNED BY public.books.id;


--
-- Name: club_memberships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.club_memberships (
    id bigint NOT NULL,
    club_id bigint NOT NULL,
    invited_by bigint,
    joined_at timestamp(6) without time zone,
    role character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT club_memberships_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'MODERATOR'::character varying, 'MEMBER'::character varying])::text[]))),
    CONSTRAINT club_memberships_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'DECLINED'::character varying, 'BANNED'::character varying])::text[])))
);


--
-- Name: club_memberships_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.club_memberships_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: club_memberships_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.club_memberships_id_seq OWNED BY public.club_memberships.id;


--
-- Name: club_posts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.club_posts (
    id bigint NOT NULL,
    author_id bigint NOT NULL,
    club_id bigint NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) without time zone,
    insightful_count integer,
    is_edited boolean,
    is_pinned boolean,
    parent_post_id bigint,
    updated_at timestamp(6) without time zone
);


--
-- Name: club_posts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.club_posts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: club_posts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.club_posts_id_seq OWNED BY public.club_posts.id;


--
-- Name: follows; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.follows (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    follower_id bigint NOT NULL,
    following_id bigint NOT NULL
);


--
-- Name: follows_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.follows_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: follows_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.follows_id_seq OWNED BY public.follows.id;


--
-- Name: post_insightfuls; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.post_insightfuls (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    post_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: post_insightfuls_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.post_insightfuls_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: post_insightfuls_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.post_insightfuls_id_seq OWNED BY public.post_insightfuls.id;


--
-- Name: reading_clubs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reading_clubs (
    id bigint NOT NULL,
    cover_image_url character varying(255),
    created_at timestamp(6) without time zone,
    created_by bigint,
    current_book_id bigint,
    description character varying(255),
    focus character varying(255),
    is_private boolean,
    name character varying(255) NOT NULL,
    next_meeting_at timestamp(6) without time zone,
    meeting_link character varying(1000)
);


--
-- Name: reading_clubs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reading_clubs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reading_clubs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.reading_clubs_id_seq OWNED BY public.reading_clubs.id;


--
-- Name: review_content_warnings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.review_content_warnings (
    review_id bigint NOT NULL,
    warning character varying(255)
);


--
-- Name: review_helpfuls; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.review_helpfuls (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    review_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: review_helpfuls_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.review_helpfuls_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: review_helpfuls_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.review_helpfuls_id_seq OWNED BY public.review_helpfuls.id;


--
-- Name: review_moods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.review_moods (
    review_id bigint NOT NULL,
    mood character varying(255)
);


--
-- Name: review_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.review_tags (
    review_id bigint NOT NULL,
    tag character varying(255)
);


--
-- Name: reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reviews (
    id bigint NOT NULL,
    book_id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    detailed_review text,
    has_spoiler boolean,
    helpful_count integer,
    pacing character varying(255),
    rating integer NOT NULL,
    spoiler_content text,
    updated_at timestamp(6) without time zone,
    user_id bigint NOT NULL,
    verdict text NOT NULL,
    who_is_it_for text,
    who_is_it_not_for text,
    CONSTRAINT reviews_pacing_check CHECK (((pacing)::text = ANY ((ARRAY['SLOW'::character varying, 'MEDIUM'::character varying, 'FAST'::character varying])::text[])))
);


--
-- Name: reviews_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reviews_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.reviews_id_seq OWNED BY public.reviews.id;


--
-- Name: user_book_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_book_status (
    id bigint NOT NULL,
    book_id bigint NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id bigint NOT NULL,
    CONSTRAINT user_book_status_status_check CHECK (((status)::text = ANY ((ARRAY['WANT_TO_READ'::character varying, 'READING'::character varying, 'READ'::character varying, 'ABANDONED'::character varying])::text[])))
);


--
-- Name: user_book_status_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_book_status_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_book_status_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_book_status_id_seq OWNED BY public.user_book_status.id;


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    role character varying(255),
    CONSTRAINT user_roles_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'CURATOR'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: user_social_links; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_social_links (
    user_id bigint NOT NULL,
    url character varying(500),
    link_order integer NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    avatar_url character varying(255),
    created_at timestamp(6) without time zone,
    email character varying(255) NOT NULL,
    enabled boolean NOT NULL,
    password character varying(255) NOT NULL,
    username character varying(255) NOT NULL,
    about_me character varying(255),
    display_name character varying(120)
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: activity_events id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activity_events ALTER COLUMN id SET DEFAULT nextval('public.activity_events_id_seq'::regclass);


--
-- Name: books id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.books ALTER COLUMN id SET DEFAULT nextval('public.books_id_seq'::regclass);


--
-- Name: club_memberships id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.club_memberships ALTER COLUMN id SET DEFAULT nextval('public.club_memberships_id_seq'::regclass);


--
-- Name: club_posts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.club_posts ALTER COLUMN id SET DEFAULT nextval('public.club_posts_id_seq'::regclass);


--
-- Name: follows id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.follows ALTER COLUMN id SET DEFAULT nextval('public.follows_id_seq'::regclass);


--
-- Name: post_insightfuls id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_insightfuls ALTER COLUMN id SET DEFAULT nextval('public.post_insightfuls_id_seq'::regclass);


--
-- Name: reading_clubs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reading_clubs ALTER COLUMN id SET DEFAULT nextval('public.reading_clubs_id_seq'::regclass);


--
-- Name: review_helpfuls id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_helpfuls ALTER COLUMN id SET DEFAULT nextval('public.review_helpfuls_id_seq'::regclass);


--
-- Name: reviews id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews ALTER COLUMN id SET DEFAULT nextval('public.reviews_id_seq'::regclass);


--
-- Name: user_book_status id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_book_status ALTER COLUMN id SET DEFAULT nextval('public.user_book_status_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: activity_events activity_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activity_events
    ADD CONSTRAINT activity_events_pkey PRIMARY KEY (id);


--
-- Name: books books_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT books_pkey PRIMARY KEY (id);


--
-- Name: club_memberships club_memberships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.club_memberships
    ADD CONSTRAINT club_memberships_pkey PRIMARY KEY (id);


--
-- Name: club_posts club_posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.club_posts
    ADD CONSTRAINT club_posts_pkey PRIMARY KEY (id);


--
-- Name: follows follows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.follows
    ADD CONSTRAINT follows_pkey PRIMARY KEY (id);


--
-- Name: post_insightfuls post_insightfuls_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_insightfuls
    ADD CONSTRAINT post_insightfuls_pkey PRIMARY KEY (id);


--
-- Name: reading_clubs reading_clubs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reading_clubs
    ADD CONSTRAINT reading_clubs_pkey PRIMARY KEY (id);


--
-- Name: review_helpfuls review_helpfuls_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_helpfuls
    ADD CONSTRAINT review_helpfuls_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- Name: club_memberships uk44i3duugsn07ysnrrktuoc070; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.club_memberships
    ADD CONSTRAINT uk44i3duugsn07ysnrrktuoc070 UNIQUE (club_id, user_id);


--
-- Name: follows uk4faelgsm2rxl2jf3iyjy981ro; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.follows
    ADD CONSTRAINT uk4faelgsm2rxl2jf3iyjy981ro UNIQUE (follower_id, following_id);


--
-- Name: reviews uk8dwwvmbh89prx5sbwddb860tc; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT uk8dwwvmbh89prx5sbwddb860tc UNIQUE (user_id, book_id);


--
-- Name: users uk_6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: books uk_books_normalized_title_author; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT uk_books_normalized_title_author UNIQUE (normalized_title, normalized_author);


--
-- Name: users uk_r43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_r43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: review_helpfuls ukd88hp387ee89e1dko4epab2hk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_helpfuls
    ADD CONSTRAINT ukd88hp387ee89e1dko4epab2hk UNIQUE (review_id, user_id);


--
-- Name: user_book_status uki7bttrvfy4aalhbgp0ehqkqbh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_book_status
    ADD CONSTRAINT uki7bttrvfy4aalhbgp0ehqkqbh UNIQUE (user_id, book_id);


--
-- Name: books ukqoslk1ksedhlok2qe8fw82dal; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT ukqoslk1ksedhlok2qe8fw82dal UNIQUE (normalized_title, author);


--
-- Name: post_insightfuls ukrcb3c1g664yq2apxpee7emml4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_insightfuls
    ADD CONSTRAINT ukrcb3c1g664yq2apxpee7emml4 UNIQUE (post_id, user_id);


--
-- Name: user_book_status user_book_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_book_status
    ADD CONSTRAINT user_book_status_pkey PRIMARY KEY (id);


--
-- Name: user_social_links user_social_links_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_social_links
    ADD CONSTRAINT user_social_links_pkey PRIMARY KEY (user_id, link_order);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: user_social_links fker5sa58pw0wccnomrwoks89r2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_social_links
    ADD CONSTRAINT fker5sa58pw0wccnomrwoks89r2 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: review_tags fkg1dakl1b69tatg7gfwhs11nml; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_tags
    ADD CONSTRAINT fkg1dakl1b69tatg7gfwhs11nml FOREIGN KEY (review_id) REFERENCES public.reviews(id);


--
-- Name: user_roles fkhfh9dx7w3ubf1co1vdev94g3f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: review_content_warnings fkqxub9bwnuxpl1jwrscx8xghvt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_content_warnings
    ADD CONSTRAINT fkqxub9bwnuxpl1jwrscx8xghvt FOREIGN KEY (review_id) REFERENCES public.reviews(id);


--
-- Name: review_moods fkrcmvqufc41wbkr3yjco1dmu55; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review_moods
    ADD CONSTRAINT fkrcmvqufc41wbkr3yjco1dmu55 FOREIGN KEY (review_id) REFERENCES public.reviews(id);


--
-- Name: book_genres fktqnlma9c5byf3gfqsuu0ebrl5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_genres
    ADD CONSTRAINT fktqnlma9c5byf3gfqsuu0ebrl5 FOREIGN KEY (book_id) REFERENCES public.books(id);


--
-- PostgreSQL database dump complete
--


