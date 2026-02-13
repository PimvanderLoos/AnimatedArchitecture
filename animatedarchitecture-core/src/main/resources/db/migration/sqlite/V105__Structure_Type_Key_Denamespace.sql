UPDATE Structure
SET type = CASE type
    WHEN 'animatedarchitecture:bigdoor' THEN 'bigdoor'
    WHEN 'animatedarchitecture:clock' THEN 'clock'
    WHEN 'animatedarchitecture:drawbridge' THEN 'drawbridge'
    WHEN 'animatedarchitecture:flag' THEN 'flag'
    WHEN 'animatedarchitecture:garagedoor' THEN 'garagedoor'
    WHEN 'animatedarchitecture:portcullis' THEN 'portcullis'
    WHEN 'animatedarchitecture:revolvingdoor' THEN 'revolvingdoor'
    WHEN 'animatedarchitecture:slidingdoor' THEN 'slidingdoor'
    WHEN 'animatedarchitecture:windmill' THEN 'windmill'
    ELSE type
END
WHERE type IN (
    'animatedarchitecture:bigdoor',
    'animatedarchitecture:clock',
    'animatedarchitecture:drawbridge',
    'animatedarchitecture:flag',
    'animatedarchitecture:garagedoor',
    'animatedarchitecture:portcullis',
    'animatedarchitecture:revolvingdoor',
    'animatedarchitecture:slidingdoor',
    'animatedarchitecture:windmill'
);
