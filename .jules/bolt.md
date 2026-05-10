## 2024-05-24 - Avoid Math.pow(x, 2) for simple squaring
**Learning:** In highly mathematical or performance-critical sections (like calculating distances or radii for animations running per tick), using `Math.pow(x, 2)` incurs unnecessary method call overhead and native implementation logic designed for arbitrary floating-point exponents. Squaring by direct multiplication `x * x` is significantly faster.
**Action:** Always use direct multiplication `x * x` instead of `Math.pow(x, 2)` in tight loops or math utility classes.
