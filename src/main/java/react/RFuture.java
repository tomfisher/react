//
// React - a library for functional-reactive-like programming
// Copyright (c) 2013, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/react/blob/master/LICENSE

package react;

/**
 * Represents an asynchronous result. Unlike standard Java futures, you cannot block on this
 * result. You can {@link #map} or {@link #flatMap} it, and listen for success or failure via the
 * {@link #success} and {@link #failure} signals.
 *
 * <p> The benefit over just using {@link Callback} is that results can be composed. You can
 * subscribe to an object, flatmap the result into a service call on that object which returns the
 * address of another object, flat map that into a request to subscribe to that object, and finally
 * pass the resulting object to some other code via a slot. Failure can be handled once for all of
 * these operations and you avoid nesting yourself three callbacks deep. </p>
 */
public class RFuture<T> {

    /** Returns a future with a pre-existing success value. */
    public static <T> RFuture<T> success (T value) {
        return result(Try.success(value));
    }

    /** Returns a future result for a {@code Void} method. */
    public static RFuture<Void> success () {
        return success(null);
    }

    /** Returns a future with a pre-existing failure value. */
    public static <T> RFuture<T> failure (Throwable cause) {
        return result(Try.<T>failure(cause));
    }

    /** Returns a future with an already-computed result. */
    public static <T> RFuture<T> result (Try<T> result) {
        return new RFuture<T>(Value.create(result));
    }

    /** Causes {@code slot} to be notified if/when this future is completed with success. If it has
     * already suceeded, the slot will be notified immediately.
     * @return this future for chaining. */
    public RFuture<T> onSuccess (final Slot<? super T> slot) {
        Try<T> result = _result.get();
        if (result == null) _result.connect(new Slot<Try<T>>() {
            public void onEmit (Try<T> result) {
                if (result.isSuccess()) slot.onEmit(result.get());
            }
        });
        else if (result.isSuccess()) slot.onEmit(result.get());
        return this;
    }

    /** Causes {@code slot} to be notified if/when this future is completed with failure. If it has
     * already failed, the slot will be notified immediately.
     * @return this future for chaining. */
    public RFuture<T> onFailure (final Slot<? super Throwable> slot) {
        Try<T> result = _result.get();
        if (result == null) _result.connect(new Slot<Try<T>>() {
            public void onEmit (Try<T> result) {
                if (result.isFailure()) slot.onEmit(result.getFailure());
            }
        });
        else if (result.isFailure()) slot.onEmit(result.getFailure());
        return this;
    };

    /** Causes {@code slot} to be notified when this future is completed. If it has already
     * completed, the slot will be notified immediately.
     * @return this future for chaining. */
    public RFuture<T> onComplete (final Slot<? super Try<T>> slot) {
        Try<T> result = _result.get();
        if (result == null) _result.connect(slot);
        else slot.onEmit(result);
        return this;
    };

    /** Returns a value that indicates whether this future has completed. */
    public ValueView<Boolean> isComplete () {
        if (_isComplete == null) _isComplete = _result.map(Functions.NON_NULL);
        return _isComplete;
    }

    /** Convenience method to {@link ValueView#connectNotify} {@code slot} to {@link #isComplete}.
     * This is useful for binding the disabled state of UI elements to this future's completeness
     * (i.e. disabled while the future is incomplete, then reenabled when it is completed).
     * @return this future for chaining. */
    public RFuture<T> bindComplete (Slot<Boolean> slot) {
        isComplete().connectNotify(slot);
        return this;
    }

    /** Maps the value of a successful result using {@link #func} upon arrival. */
    public <R> RFuture<R> map (final Function<? super T, R> func) {
        // we'd use Try.lift here but we have to handle the special case where our try is null,
        // meaning we haven't completed yet; it would be weird if Try.lift did that
        return new RFuture<R>(_result.map(new Function<Try<T>,Try<R>>() {
            public Try<R> apply (Try<T> result) {
                return result == null ? null : result.map(func);
            }
        }));
    };

    /** Maps a successful result to a new result using {@link #func} when it arrives. Failure on
     * the original result or the mapped result are both dispatched to the mapped result. This is
     * useful for chaining asynchronous actions. It's also known as monadic bind. */
    public <R> RFuture<R> flatMap (final Function<? super T, RFuture<R>> func) {
        final Value<Try<R>> mapped = Value.create(null);
        _result.connectNotify(new Slot<Try<T>>() {
            public void onEmit (Try<T> result) {
                if (result == null) return; // source future not yet complete; nothing to do
                if (result.isFailure()) mapped.update(Try.<R>failure(result.getFailure()));
                else func.apply(result.get()).onComplete(mapped.slot());
            }
        });
        return new RFuture<R>(mapped);
    };

    protected RFuture (ValueView<Try<T>> result) {
        _result = result;
    }

    protected final ValueView<Try<T>> _result;
    protected ValueView<Boolean> _isComplete;
}