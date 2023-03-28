# ring-partial-content

Ring middleware to provide Partial Content responses as described in
RFC2616 section 10.2.7.


## Usage

```clojure
(use 'ring.middleware.partial-content)
(def app (-> routes .. wrap-partial-content))
```


## Installation

Drop the following dependency in your `project.clj` at the
appropriate place:

```clojure
[ring-partial-content "2.0.1"]
```

## Contribute

Send bug reports and patches to
[~rwv/public-inbox@lists.sr.ht](mailto:~rwv/public-inbox@lists.sr.ht).

## License

Copyright (c) Remco van 't Veer. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0
(http://opensource.org/licenses/eclipse-1.0.php) which can be found in
the file epl-v10.html at the root of this distribution.  By using this
software in any fashion, you are agreeing to be bound by the terms of
this license.  You must not remove this notice, or any other, from
this software.
