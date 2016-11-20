/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.semeniuc.dmitrii.clientmanager.appointments;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.ViewGroup;

import com.semeniuc.dmitrii.clientmanager.addeditappointment.AddEditAppointmentActivity;
import com.semeniuc.dmitrii.clientmanager.data.local.AppointmentsDataSource;
import com.semeniuc.dmitrii.clientmanager.model.Appointment;
import com.semeniuc.dmitrii.clientmanager.repository.AppointmentRepository;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link AppointmentsFragment}), retrieves the data and
 * updates the UI as required.
 */
public class AppointmentsPresenterImpl implements AppointmentsPresenter {

    private AppointmentRepository appointmentsRepository;
    private AppointmentsView appointmentsView;
    private AppointmentsFilterType currentFiltering = AppointmentsFilterType.ALL_APPOINTMENTS;
    private boolean firstLoad = true;

    public AppointmentsPresenterImpl(@NonNull AppointmentsView appointmentsView) {
        this.appointmentsRepository = new AppointmentRepository();
        this.appointmentsView = checkNotNull(appointmentsView, "appointmentsView cannot be null!");
        //this.appointmentsRepository = appointmentsRepository;
        //this.appointmentsView = appointmentsView;

        //appointmentsView.setPresenter(this);
    }

    @Override
    public void start() {
        loadAppointments(false);
    }

    @Override public void result(int requestCode, int resultCode) {
        // If an appointment was successfully added, show snackbar
        if (AddEditAppointmentActivity.REQUEST_ADD_APPOINTMENT == requestCode && Activity.RESULT_OK == resultCode) {
            appointmentsView.showSuccessfullySavedMessage();
        }
    }

    @Override public void loadAppointments(boolean forceUpdate) {
        loadAppointments(forceUpdate || firstLoad, true);
        firstLoad = false;
    }

    /**
     * @param forceUpdate   Pass in true to refresh the data in the {@link AppointmentsDataSource}
     * @param showLoadingUI Pass in true to display a loading icon in the UI
     */
    private void loadAppointments(boolean forceUpdate, final boolean showLoadingUI) {
        if (showLoadingUI) {
           appointmentsView.setLoadingIndicator(true);
        }
        if (forceUpdate) {
            appointmentsRepository.refreshAppointments(); // TODO:
        }

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
     //   EspressoIdlingResource.increment(); // App is busy until further notice

        appointmentsRepository.getAppointment(new AppointmentsDataSource.LoadAppointmentsCallback() {
            @Override
            public void onAppointmentsLoaded(List<Appointment> appointments) {
                List<Appointment> appointmentsToShow = new ArrayList<>();

                // This callback may be called twice, once for the cache and once for loading
                // the data from the server API, so we check before decrementing, otherwise
                // it throws "Counter has been corrupted!" exception.
              /*  if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                    EspressoIdlingResource.decrement(); // Set app as idle.
                }*/

                // We filter the tasks based on the requestType
                for (Appointment appointment : appointments) {
                    switch (currentFiltering) {
                        case ALL_APPOINTMENTS:
                            appointmentsToShow.add(appointment);
                            break;
                        case ACTIVE_APPOINTMENTS:
                            if (appointment.isActive())
                                appointmentsToShow.add(appointment);
                            break;
                        case COMPLETED_APPOINTMENTS:
                            if (appointment.isCompleted()) {
                                appointmentsToShow.add(appointment);
                            }
                            break;
                        default:
                            appointmentsToShow.add(appointment);
                            break;
                    }
                }
                // The view may not be able to handle UI updates anymore
                if (!appointmentsView.isActive()) {
                    return;
                }
                if (showLoadingUI) {
                    appointmentsView.setLoadingIndicator(false);
                }

                processAppointments(appointmentsToShow);
            }

            @Override
            public void onDataNotAvailable() {
                // The view may not be able to handle UI updates anymore
                if (!appointmentsView.isActive()) {
                    return;
                }
                appointmentsView.showLoadingAppointmentsError();
            }
        });
    }

    private void processAppointments(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            // Show a message indicating there are no tasks for that filter type.
            processEmptyAppointments();
        } else {
            // Show the list of tasks
            appointmentsView.showAppointments(appointments);
            // Set the filter label's text.
            showFilterLabel();
        }
    }

    private void processEmptyAppointments() {
        switch (currentFiltering) {
            case ACTIVE_APPOINTMENTS:
                appointmentsView.showNoActiveAppointments();
                break;
            case COMPLETED_APPOINTMENTS:
                appointmentsView.showNoCompletedAppointments();
                break;
            default:
                appointmentsView.showNoAppointments();
                break;
        }
    }

    private void showFilterLabel() {
        switch (currentFiltering) {
            case ACTIVE_APPOINTMENTS:
                appointmentsView.showActiveFilterLabel();
                break;
            case COMPLETED_APPOINTMENTS:
                appointmentsView.showCompletedFilterLabel();
                break;
            default:
                appointmentsView.showAllFilterLabel();
                break;
        }
    }

    @Override public void addNewAppointment() {
        appointmentsView.showAddAppointment();
    }

    @Override public void openAppointmentDetails(@NonNull Appointment requestedAppointment) {
        checkNotNull(requestedAppointment, "requestedAppointment cannot be null!");
     //   appointmentsView.showAppointmentDetailsUi(requestedAppointment.getId());
    }

    @Override public void completeAppointment(@NonNull Appointment completedAppointment) {
        checkNotNull(completedAppointment, "completedAppointment cannot be null!");
    //    appointmentsRepository.completeAppointment(completedAppointment);
        appointmentsView.showAppointmentMarkedComplete();
        loadAppointments(false, false);
    }

    @Override public void activateAppointment(@NonNull Appointment activeAppointment) {
        checkNotNull(activeAppointment, "activeAppointment cannot be null!");
     //   appointmentsRepository.activateAppointment(activeAppointment);
        appointmentsView.showAppointmentMarkedActive();
        loadAppointments(false, false);
    }

    @Override public void clearCompletedAppointments() {
        appointmentsRepository.clearCompletedAppointments();
        appointmentsView.showCompletedAppointmentsCleared();
        loadAppointments(false, false);
    }

    @Override public void setFiltering(AppointmentsFilterType requestType) {
        currentFiltering = requestType;
    }

    @Override public AppointmentsFilterType getFiltering() {
        return currentFiltering;
    }

    @Override public void hideKeyboard(ViewGroup layout) {
    }
}